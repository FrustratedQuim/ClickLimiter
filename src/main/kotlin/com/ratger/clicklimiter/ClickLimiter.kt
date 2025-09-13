package com.ratger.clicklimiter

import org.bukkit.plugin.java.JavaPlugin

class ClickLimiter : JavaPlugin() {
    private lateinit var configManager: ConfigManager
    private lateinit var clickManager: ClickManager

    override fun onEnable() {
        configManager = ConfigManager(this)
        configManager.loadConfig()

        clickManager = ClickManager(this, configManager)
        server.pluginManager.registerEvents(clickManager, this)

        getCommand("cl-reload")?.setExecutor(CommandManager(configManager))

        logger.info("ClickLimiter enabled. Max CPS: ${configManager.getMaxCpsValue()}")
    }

    override fun onDisable() {
        logger.info("ClickLimiter disabled.")
    }
}
