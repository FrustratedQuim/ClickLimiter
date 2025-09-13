package com.ratger.clicklimiter

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayDeque
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.getOrPut
import kotlin.collections.isNotEmpty
import kotlin.collections.set

class ClickManager(plugin: JavaPlugin, private val configManager: ConfigManager) : Listener {
    private val diggingPlayers = ConcurrentHashMap.newKeySet<UUID>()
    private val lastAnimation : MutableMap<UUID, Long> = ConcurrentHashMap()
    private val clickWindows : MutableMap<UUID, ClickWindow> = ConcurrentHashMap()

    private val resetTime : Long = 60_000

    init {
        registerPacketListener()

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            val now = System.currentTimeMillis()
            clickWindows.entries.removeIf { (uuid, window) ->
                val lastClick = window.getLastClickTime()
                lastClick != null && now - lastClick > resetTime
            }
            lastAnimation.entries.removeIf { now - it.value > resetTime }
        }, 0L, resetTime / 50)
    }

    private fun registerPacketListener() {
        val listener = object : PacketListenerAbstract(PacketListenerPriority.LOWEST) {
            override fun onPacketReceive(event: PacketReceiveEvent?) {
                val type = event?.packetType ?: return
                val player = event.getPlayer() as? Player ?: return
                val uuid = player.uniqueId
                val now = System.currentTimeMillis()

                when (type) {

                    // Анимация удара начинает непрерывно прокидываться при копании, поэтому подобное нужно исключить из учёта
                    PacketType.Play.Client.PLAYER_DIGGING -> {
                        // Игрок в креативе прокидывает лишь пакет START_DIGGING
                        if (player.gameMode == GameMode.CREATIVE) return

                        val packet = WrapperPlayClientPlayerDigging(event)
                        when (packet.action) {
                            DiggingAction.START_DIGGING -> diggingPlayers.add(uuid)
                            DiggingAction.CANCELLED_DIGGING, DiggingAction.FINISHED_DIGGING -> diggingPlayers.remove(uuid)
                            else -> {}
                        }
                    }
                    PacketType.Play.Client.ANIMATION -> {
                        if (uuid in diggingPlayers) return
                        lastAnimation[uuid] = now
                        handleClick(player) { event.isCancelled = true }
                    }
                }
            }
        }
        PacketEvents.getAPI().eventManager.registerListener(listener)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        clickWindows.remove(event.player.uniqueId)
        diggingPlayers.remove(event.player.uniqueId)
        lastAnimation.remove(event.player.uniqueId)
    }

    private fun handleClick(player: Player, cancelAction: () -> Unit) {
        val uuid = player.uniqueId
        val now = System.currentTimeMillis()
        val window = clickWindows.getOrPut(uuid) { ClickWindow() }
        val maxCps = configManager.getMaxCpsValue()

        val cps = window.getCps(now)
        if (cps >= maxCps) {
            val lastClickTime = window.getLastClickTime()
            if (lastClickTime != null && now - lastClickTime < 1000 / maxCps) {
                cancelAction()
                return
            }
        }

        window.addClick(now)
    }

    class ClickWindow {
        private val clickTimes = ArrayDeque<Long>()
        private var cachedCps: Int = 0
        private var lastCleanup: Long = 0

        fun addClick(now: Long) {
            synchronized(clickTimes) {
                clickTimes.add(now)
                cleanOldClicks(now)
                cachedCps = clickTimes.size
            }
        }

        fun getCps(now: Long): Int {
            synchronized(clickTimes) {
                if (now - lastCleanup >= 1000) {
                    cleanOldClicks(now)
                    cachedCps = clickTimes.size
                }
                return clickTimes.size
            }
        }

        private fun cleanOldClicks(now: Long) {
            while (clickTimes.isNotEmpty() && clickTimes.first() < now - 1000) {
                clickTimes.removeFirst()
            }
            lastCleanup = now
        }

        fun getLastClickTime(): Long? {
            synchronized(clickTimes) {
                return clickTimes.lastOrNull()
            }
        }
    }
}