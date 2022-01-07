package com.astrainteractive.empire_items.empire_items.api.mobs.data

import com.astrainteractive.empire_items.empire_items.api.items.data.AstraItem.Companion.getIntOrNull
import org.bukkit.configuration.ConfigurationSection

data class EmpireMobEvent(
    val eventName:String,
    val hitAfter:Long?,
    val range:Int?,
    val decreaseDamageByRange:Boolean,
    val sound:String?,
    val cooldown:Long?,
    val animation:String?,
    val bones:List<BoneInfo>
){
    companion object{
        fun get(s:ConfigurationSection?): List<EmpireMobEvent> {
            return s?.getKeys(false)?.mapNotNull { key->
                fromSection(s.getConfigurationSection(key))
            }?: listOf()
        }
        fun fromSection(s:ConfigurationSection?):EmpireMobEvent?{
            s?:return null
            return EmpireMobEvent(
                eventName= s.getString("eventName")?:s.name,
                hitAfter = s.getIntOrNull("hitAfter")?.toLong(),
                range = s.getIntOrNull("range"),
                cooldown = s.getIntOrNull("cooldown")?.toLong(),
                decreaseDamageByRange = s.getBoolean("decreaseDamageByRange"),
                sound = s.getString("sound"),
                animation = s.getString("animation"),
                bones = BoneInfo.getBones(s.getConfigurationSection("bones"))
            )
        }
    }
}
