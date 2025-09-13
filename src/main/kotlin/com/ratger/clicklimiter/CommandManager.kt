package com.ratger.clicklimiter

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandManager(private val configManager: ConfigManager) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command, label: String,
        args: Array<out String>
    ): Boolean {
        if (command.name.equals("cl-reload", ignoreCase = true)) {
            if (sender !is Player || !sender.hasPermission("clicklimiter.reload")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<dark_red>▍ <#FF1500>У вас недостаточно прав!"))
                return true
            }

            configManager.reloadConfig()
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<dark_green>▍ <#00FF40>Максимальный CPS обновлён: ${configManager.getMaxCpsValue()}"))
            return true
        }
        return false
    }
}