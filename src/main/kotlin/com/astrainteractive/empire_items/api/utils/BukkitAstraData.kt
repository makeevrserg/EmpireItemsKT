package com.astrainteractive.empire_items.api.utils

import com.astrainteractive.astralibs.AstraLibs
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.persistence.PersistentDataType


data class BukkitConstant<T, Z>(
    val value: NamespacedKey,
    val dataType: PersistentDataType<T, Z>
) {
    constructor(key: String, dataType: PersistentDataType<T, Z>) : this(
        NamespacedKey(AstraLibs.instance, key),
        dataType
    )
}

object BukkitConstants {
    fun ASTRA_ID() = BukkitConstant(NamespacedKey(AstraLibs.instance, "ASTRA_ID"), PersistentDataType.STRING)
    fun ASTRA_ATTRIBUTE(attr: Attribute) =
        BukkitConstant(
            NamespacedKey(AstraLibs.instance, ASTRA_UPGRADE + attr.name.lowercase()),
            PersistentDataType.DOUBLE
        )

    object EmpireEnchants {
        val EMPIRE_ENCHANT
            get() = BukkitConstant("EMPIRE_ENCHANT", PersistentDataType.BYTE)
        val LAVA_WALKER_ENCHANT
            get() = BukkitConstant("LAVA_WALKER_ENCHANT", PersistentDataType.INTEGER)
        val VAMPIRISM
            get() = BukkitConstant("VAMPIRISM", PersistentDataType.INTEGER)
        val FROST_ASPECT
            get() = BukkitConstant("FROST_ASPECT", PersistentDataType.INTEGER)
        val BUTCHER
            get() = BukkitConstant("BUTCHER", PersistentDataType.INTEGER)
        val VYDERLIGHT
            get() = BukkitConstant("VYDERLIGHT", PersistentDataType.INTEGER)
        val AQUALIGHT
            get() = BukkitConstant("AQUALIGHT", PersistentDataType.INTEGER)
        val STRIKE
            get() = BukkitConstant("STRIKE", PersistentDataType.INTEGER)

        val enchant: List<BukkitConstant<Int, Int>>
            get() = listOf(LAVA_WALKER_ENCHANT, VAMPIRISM, STRIKE, FROST_ASPECT, BUTCHER, VYDERLIGHT,AQUALIGHT)
        val enchantByName: Map<String, BukkitConstant<Int, Int>>
            get() = enchant.associateBy { it.value.key.uppercase() }

    }

    //Кастомные эвенты
    val MOLOTOV
        get() = BukkitConstant("MOLOTOV", PersistentDataType.INTEGER)
    val GRAPPLING_HOOK
        get() = BukkitConstant("GRAPPLING_HOOK", PersistentDataType.STRING)
    val SOUL_BIND
        get() = BukkitConstant("SOUL_BIND", PersistentDataType.INTEGER)
    val HAMMER_ENCHANT
        get() = BukkitConstant("HAMMER_ENCHANT", PersistentDataType.INTEGER)
    val GRENADE_EXPLOSION_POWER
        get() = BukkitConstant("GRENADE_EXPLOSION_POWER", PersistentDataType.INTEGER)
    val EMPIRE_DURABILITY
        get() = BukkitConstant("EMPIRE_DURABILITY", PersistentDataType.INTEGER)
    val MAX_CUSTOM_DURABILITY
        get() = BukkitConstant("MAX_CUSTOM_DURABILITY", PersistentDataType.INTEGER)
    val CORE_INSPECT
        get() = BukkitConstant("CORE_INSPECT", PersistentDataType.INTEGER)
    val VOID_TOTEM
        get() = BukkitConstant("VOID_TOTEM", PersistentDataType.STRING)
    val TOTEM_OF_DEATH
        get() = BukkitConstant("TOTEM_OF_DEATH", PersistentDataType.STRING)
    val CRAFT_DURABILITY
        get() = BukkitConstant("CRAFT_DURABILITY", PersistentDataType.INTEGER)
    val CLIP_SIZE
        get() = BukkitConstant("CLIP_SIZE", PersistentDataType.INTEGER)

    val SLIME_CATCHER
        get() = BukkitConstant("SLIME_CATCHER", PersistentDataType.STRING)

    //Апгрейд
    val ASTRA_UPGRADE_TIMES
        get() = BukkitConstant(
            NamespacedKey(AstraLibs.instance, "ASTRA_UPGRADE_TIMES".lowercase()),
            PersistentDataType.INTEGER
        )
    val ASTRA_UPGRADE
        get() = "ASTRA_UPGRADE_".lowercase()
    val ASTRA_CRAFTING
        get() = "ASTRA_CRAFTING".lowercase()

}
