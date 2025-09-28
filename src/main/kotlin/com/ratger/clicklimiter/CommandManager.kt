package com.ratger.clicklimiter

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class CommandManager(
    private val configManager: ConfigManager,
    private val clickManager: ClickManager
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command, label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player || !sender.hasPermission("clicklimiter.set")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<dark_red>▍ <#FF1500>У вас недостаточно прав!"))
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<dark_red>▍ <#FF1500>Использование: /cl-set <max_cps>"))
            return true
        }

        var newValue = args[0].toIntOrNull()
        if (newValue == null || newValue < 1) newValue = 16

        configManager.updateConfig(newValue, true)
        clickManager.updateData()

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<dark_green>▍ <#00FF40>Максимальный CPS обновлён: $newValue"))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission("clicklimiter.set") || args.size != 1) {
            return emptyList()
        }
        return listOf( "2", "8", "16", "32" )
    }
}