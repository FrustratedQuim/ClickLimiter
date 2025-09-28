package com.ratger.clicklimiter

import org.bukkit.plugin.java.JavaPlugin

class ClickLimiter : JavaPlugin() {
    private lateinit var clickManager: ClickManager
    private lateinit var configManager: ConfigManager

    override fun onEnable() {
        configManager = ConfigManager(this)
        clickManager = ClickManager(configManager)

        configManager.updateConfig()

        getCommand("cl-set")?.setExecutor(CommandManager(configManager, clickManager))
        getCommand("cl-set")?.tabCompleter = CommandManager(configManager, clickManager)

        server.pluginManager.registerEvents(clickManager, this)
        logger.info("ClickLimiter enabled. Max CPS: ${configManager.getMaxCpsValue()}")
    }
}
