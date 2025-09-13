package com.ratger.clicklimiter

import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ConfigManager(private val plugin: JavaPlugin) {
    private var maxCpsValue: Int = 20

    fun loadConfig() {
        val configFile = File(plugin.dataFolder, "config.yml")
        if (!configFile.exists()) {
            plugin.dataFolder.mkdirs()
            plugin.saveResource("config.yml", false)
        }
        plugin.reloadConfig()
        reloadConfig()
    }

    fun reloadConfig() {
        plugin.reloadConfig()
        maxCpsValue = plugin.config.getInt("max-cps-value", 20).coerceAtLeast(1)
    }

    fun getMaxCpsValue(): Int = maxCpsValue
}