package com.ratger.clicklimiter

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import kotlin.collections.mutableMapOf

class ClickManager(private val configManager: ConfigManager) : Listener {

    private val players = mutableMapOf<UUID, UserData>()

    private var maxCpsValue = 0
    private var maxClickRate = 0

    init {
        registerPacketListener()
        updateData()
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val uuid = event.player.uniqueId
        val isBypass = event.player.hasPermission("clicklimiter.bypass")
        players[uuid] = UserData(
            UserClicks(maxCpsValue),
            false,
            isBypass
        )
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        players.remove(event.player.uniqueId)
    }

    private fun registerPacketListener() {
        val listener = object : PacketListenerAbstract(PacketListenerPriority.LOWEST) {
            override fun onPacketReceive(event: PacketReceiveEvent) {
                val type = event.packetType
                // Пакет ANIMATION провоцирует лишь левый клик
                if (type != PacketType.Play.Client.ANIMATION && type != PacketType.Play.Client.PLAYER_DIGGING) return

                val player = event.getPlayer() as? Player ?: return
                val uuid = player.uniqueId

                if (players[uuid]!!.isBypass) return

                // Пакет копания всегда идёт парой - при начале и конце, поэтому подобная проверка безопасна.
                // Игрок в креативе пропускается, так-как в его случае анимации копания нет и отсылается лишь пакет начала.
                if (type == PacketType.Play.Client.PLAYER_DIGGING) {
                    if (player.gameMode != GameMode.CREATIVE) {
                        players[uuid]!!.isDigging = !players[uuid]!!.isDigging
                        return
                    }
                }

                if (!players[uuid]!!.isDigging) handleClick(uuid) { event.isCancelled = true }
            }
        }
        PacketEvents.getAPI().eventManager.registerListener(listener)
    }

    private fun handleClick(uuid: UUID, cancelAction: () -> Unit) {
        val now = System.currentTimeMillis()
        val buffer = players[uuid]!!.clicks

        if (buffer.countValidClicks(now) >= maxCpsValue && now - buffer.lastClick() < maxClickRate) {
            cancelAction()
            return
        }

        buffer.add(now)
    }

    fun updateData() {
        maxCpsValue = configManager.getMaxCpsValue()
        maxClickRate = (1000 / maxCpsValue)

        players.forEach { (uuid, userData) ->
            players[uuid] = UserData(
                UserClicks(maxCpsValue),
                userData.isDigging,
                userData.isBypass
            )
        }
    }
}

private class UserData(
    val clicks: UserClicks,
    var isDigging: Boolean,
    var isBypass: Boolean
)

// Мы юзаем кольцевой буфер, чтобы не тратить операции на удаление и добавление элементов.
// Перезапись менее затратна.
private class UserClicks(size: Int) {
    val times = LongArray(size)
    var index = 0
    private var count = 0
    private var oldest = 0

    fun add(time: Long) {
        times[index] = time
        index = (index + 1) % times.size
        if (count < times.size) count++ else oldest = (oldest + 1) % times.size
    }

    // Количество кликов за последнюю секунду
    fun countValidClicks(currentTime: Long): Int {
        while (count > 0 && currentTime - times[oldest] > 1000) {
            oldest = (oldest + 1) % times.size
            count--
        }
        return count
    }

    fun lastClick(): Long = if (count == 0) 0 else times[(index - 1 + times.size) % times.size]
}