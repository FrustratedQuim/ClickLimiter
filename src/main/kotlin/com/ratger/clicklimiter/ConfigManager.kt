package com.ratger.clicklimiter

import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ConfigManager(private val plugin: JavaPlugin) {
    private var maxCpsValue: Int = plugin.config.getInt("max-cps-value", 16).coerceAtLeast(1)

    fun updateConfig(newValue: Int = 16, isUpdate: Boolean = false) {
        val path = File(plugin.dataFolder, "config.yml")
        if (!path.exists()) {
            plugin.dataFolder.mkdirs()
            plugin.saveResource("config.yml", false)
        }

        maxCpsValue = if (isUpdate) {
            plugin.config.set("max-cps-value", newValue)
            plugin.saveConfig()
            newValue
        } else {
            plugin.reloadConfig()
            plugin.config.getInt("max-cps-value", 16).coerceAtLeast(1)
        }
    }

    fun getMaxCpsValue(): Int = maxCpsValue
}