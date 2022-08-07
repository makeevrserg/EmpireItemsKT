package com.astrainteractive.empire_items.credit

import com.astrainteractive.astralibs.FileManager
import com.astrainteractive.astralibs.utils.catching
import com.earth2me.essentials.Essentials
import com.astrainteractive.empire_items.credit.commands.CommandManager
import com.astrainteractive.empire_items.credit.data.CreditConfig
import org.bukkit.Bukkit


class EmpireCredit {
    companion object {
        lateinit var instance: EmpireCredit
            private set
        lateinit var configFile: FileManager
            private set
        lateinit var essentials: Essentials
            private set
        lateinit var config: CreditConfig
            private set
    }

    private lateinit var placeholderHook:PlaceholderHook

    inline fun <reified T> getPluginDependency(name:String) = catching{
        return@catching Bukkit.getPluginManager().getPlugin("Essentials") as T
    }

    private fun initCreditSystem() {
        essentials = getPluginDependency("Essentials")!!
        instance = this
        configFile = FileManager("credit/credit.yml")
        config = CreditConfig.new()!!
        CommandManager()
        if (Bukkit.getServer().pluginManager.getPlugin("PlaceholderAPI")!=null) {
            placeholderHook = PlaceholderHook()
            placeholderHook.register()
        }




    }

    init {
        initCreditSystem()
    }
    fun onDisable(){
        if (Bukkit.getServer().pluginManager.getPlugin("PlaceholderAPI")!=null && Bukkit.getPluginManager().getPlugin("Essentials")!=null) {
            placeholderHook.unregister()
        }
    }


}