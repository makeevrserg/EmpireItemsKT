package com.astrainteractive.empireprojekt.empire_items.util

import com.astrainteractive.astralibs.catching
import com.google.gson.Gson
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

class YamlParser{
    companion object{
        val parser = YamlParser()
    }
    fun <T> fixNull(v1:T?,v2:T):T = v1?:v2
    /**
     * Convert configuration section to map
     */
    fun configurationSectionToMap(section: ConfigurationSection?): Map<String, Any?> {
        section ?: return hashMapOf<String, Any>()
        return section.getKeys(false).associateWith { key ->
            val value: Any? = if (section.isConfigurationSection(key))
                configurationSectionToMap(section.getConfigurationSection(key))
            else {
                val obj = section.get(key)
                if (obj is String)
                    "\"${obj}\""
                else obj
            }
            value
        }
    }

    /**
     * Convert configuration section to Class
     */
    inline fun <reified T> configurationSectionToClass(section: ConfigurationSection?,debugSection:Boolean=false): T? = catching {
        val map = section.toMap() ?: return null

        val stringMap = map.toString()
        if (debugSection) {
            println(map)
            println(stringMap)
        }

        return Gson().fromJson(stringMap, T::class.java)
    }

    /**
     * Convert configuration section to map
     */
    fun ConfigurationSection?.toMap() = configurationSectionToMap(this)


    /**
     * Convert file configuration to Class
     */
    inline fun <reified T> fileConfigurationToClass(file: FileConfiguration?): T? =
        configurationSectionToClass(file?.defaultSection)

    /**
     * Convert fileConfig to map
     */
    fun mapFromFileConfig(f: FileConfiguration?) =
        f?.defaultSection?.toMap()


}