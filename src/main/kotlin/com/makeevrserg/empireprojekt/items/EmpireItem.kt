package com.makeevrserg.empireprojekt.items

import com.destroystokyo.paper.ParticleBuilder
import com.makeevrserg.empireprojekt.EmpirePlugin
import com.makeevrserg.empireprojekt.util.EmpireUtils
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

fun ConfigurationSection.getHEXString(path: String): String? {
    return EmpireUtils.HEXPattern(getString(path))
}

fun ConfigurationSection.getHEXStringList(path: String): List<String> {
    return EmpireUtils.HEXPattern(getStringList(path))
}

private fun getItemFlagList(list: List<String>?): List<ItemFlag> {
    val flags = mutableListOf<ItemFlag>()
    for (flag in list ?: return mutableListOf())
        flags.add(ItemFlag.valueOf(flag))
    return flags
}

private fun getEnchantements(section: ConfigurationSection?): Map<Enchantment, Int> {
    section ?: return mutableMapOf()
    val enchantments = mutableMapOf<Enchantment, Int>()
    for (key in section.getKeys(false)) {
        val enchantment = Enchantment.getByKey(NamespacedKey.minecraft(key.lowercase())) ?: continue
        enchantments[enchantment] = section.getInt(key)
    }
    return enchantments
}

private fun getAttributes(section: ConfigurationSection?): Map<Attribute, Int> {
    section ?: return mutableMapOf()
    val map = mutableMapOf<Attribute, Int>()
    for (key in section.getKeys(false)) {
        val sect = section.getConfigurationSection(key) ?: continue
        val name = sect.getString("name") ?: continue
        val amount = sect.getInt("amount")
        val attribute = Attribute.valueOf(name)
        map[attribute] = amount
    }
    return map
}

private fun getEvents(section: ConfigurationSection?): List<Event> {
    section ?: return mutableListOf()
    val events = mutableListOf<Event>()
    for (key in section.getKeys(false)) {
        val event = Event(section.getConfigurationSection(key) ?: continue)
        events.add(event)
    }

    return events
}

private fun getEmprieEnchants(section: ConfigurationSection?):Map<String,Double>{
    section?:return mutableMapOf()
    val map = mutableMapOf<String,Double>()
    for (key in section.getKeys(false)){
        val value = section.getDouble(key)?:continue
        map[key] = value
    }
    return map
}
data class EmpireItem(
    val id: String,
    val displayName: String,
    val lore: List<String>,
    val material: Material,
    val texturePath: String?,
    val modelPath: String?,
    val customModelData: Int,
    val itemFlags: List<ItemFlag>,
    val enchants: Map<Enchantment, Int>,
    val durability: Int?,
    val attributes: Map<Attribute, Int>,
    val events: List<Event>,
    val musicDisc: String?,
    val empireGun:EmpireGun?,
    val empireEnchants:Map<String,Double>
) {
    constructor(conf: ConfigurationSection) : this(
        conf.name,
        conf.getHEXString("display_name")!!,
        conf.getHEXStringList("lore"),
        Material.getMaterial(conf.getString("material")!!)!!,
        conf.getString("texture_path"),
        conf.getString("model_path"),
        conf.getInt("custom_model_data", -1),
        getItemFlagList(conf.getStringList("item_flags")),
        getEnchantements(conf.getConfigurationSection("enchantements")),
        conf.getInt("durability"),
        getAttributes(conf.getConfigurationSection("attributes")),
        getEvents(conf.getConfigurationSection("interact")),
        conf.getString("music_disc.song"),
        EmpireGun().init(conf.getConfigurationSection("empire_gun")),
        getEmprieEnchants(conf.getConfigurationSection("empire_enchants"))
    )

    private fun ItemMeta.addItemFlags(flags: List<ItemFlag>) {
        for (flag in flags)
            this.addItemFlags(flag)
    }

    private fun ItemMeta.addEnchants(enchants: Map<Enchantment, Int>) {
        for (enchantment in enchants.keys)
            this.addEnchant(enchantment, enchants[enchantment] ?: continue, true)
    }

    private fun ItemMeta.addAttributes(attributes: Map<Attribute, Int>) {
        for (attribute in attributes.keys) {
            val amount = attributes[attribute]?.toDouble() ?: continue
            this.addAttributeModifier(
                attribute,
                AttributeModifier(
                    UUID.randomUUID(),
                    attribute.name,
                    amount,
                    AttributeModifier.Operation.ADD_NUMBER,
                    material.equipmentSlot
                )
            )

        }
    }

    private fun ItemMeta.setID(id: String) {
        this.persistentDataContainer.set(EmpirePlugin.empireConstants.empireID, PersistentDataType.STRING, id)
    }

    private fun ItemMeta.setDurability() {
        durability ?: return
        this.persistentDataContainer.set(
            EmpirePlugin.empireConstants.EMPIRE_DURABILITY,
            PersistentDataType.INTEGER,
            durability
        )
        this.persistentDataContainer.set(
            EmpirePlugin.empireConstants.MAX_CUSTOM_DURABILITY,
            PersistentDataType.INTEGER,
            durability
        )
    }

    private fun ItemMeta.setPotionMeta(): ItemMeta {
        if (material != Material.POTION)
            return this
        val potionMeta = this as PotionMeta
        potionMeta.color = Color.WHITE
        return potionMeta
    }

    private fun ItemMeta.addEmpireEnchant(){
        for (enchantKey in empireEnchants.keys){
            val value = empireEnchants[enchantKey]?:continue
            this.persistentDataContainer.set(EmpirePlugin.empireConstants.getEnchantsMap()[enchantKey]?:continue,
                PersistentDataType.DOUBLE,value)
        }
    }

    private fun ItemMeta.setFixedItem(){
            this.persistentDataContainer.set(EmpirePlugin.empireConstants.FIXED_ITEM,
                PersistentDataType.SHORT,0)

    }
    public fun getItemStack(): ItemStack? {
        val itemStack = ItemStack(material)
        var meta = itemStack.itemMeta ?: return null
        meta.setDisplayName(displayName)
        meta.lore = lore
        meta.setCustomModelData(customModelData)
        meta.addItemFlags(itemFlags)
        meta.addEnchants(enchants)
        meta.addAttributes(attributes)
        meta.persistentDataContainer.set(EmpirePlugin.empireConstants.empireID, PersistentDataType.STRING, id)
        meta.setDurability()
        meta = meta.setPotionMeta()
        meta.addEmpireEnchant()
        meta.setFixedItem()
        itemStack.itemMeta = meta
        return itemStack


    }
}

private fun getCommands(section: ConfigurationSection?): List<Command> {
    section ?: return mutableListOf()
    val list = mutableListOf<Command>()
    for (key in section.getKeys(false)) {
        val sect = section.getConfigurationSection(key) ?: continue
        list.add(Command(sect.getString("command") ?: continue, sect.getBoolean("as_console")))
    }
    return list
}

private fun getPotionEffectAdd(section: ConfigurationSection?): List<PotionEffect> {
    section ?: return mutableListOf()
    val list = mutableListOf<PotionEffect>()
    for (key in section.getKeys(false)) {
        val sect = section.getConfigurationSection(key) ?: continue
        val potionType = PotionEffectType.getByName(sect.name.lowercase()) ?: continue
        val potionEffect = PotionEffect(potionType, sect.getInt("duration"), sect.getInt("amplifier"))
        list.add(potionEffect)
    }
    return list
}

private fun getPotionEffectRemove(removeList: List<String>): List<PotionEffectType> {
    val list = mutableListOf<PotionEffectType>()
    for (key in removeList) {
        val potionType = PotionEffectType.getByName(key) ?: continue
        list.add(potionType)
    }
    return list
}

private fun getParticles(sect: ConfigurationSection?): ParticleBuilder? {
    sect ?: return null
    val name = sect.getString("name") ?: return null
    val count = sect.getInt("count")
    val time = sect.getDouble("time")
    val particle = ParticleBuilder(Particle.valueOf(name)).count(count).extra(time)

    return particle
}

private fun getSounds(section: ConfigurationSection?): Sound? {
    section ?: return null


    val name = section.getString("name") ?: return null
    val volume = section.getDouble("volume")
    val pitch = section.getDouble("pitch")


    return Sound(name, volume, pitch)
}
class EmpireGun {

    var clipSize: Int = 5
    var gunDamage: Double = 5.0
    var gunLength: Int = 5
    var gunCooldown: Double = 0.1
    var gunRecoil: Double = 1.0
    var gunBulletWeight: Double = 0.8
    lateinit var reloadBy: String
    lateinit var noAmmoSound: String
    lateinit var shootSound: String
    lateinit var reloadSound: String
    var bulletColor: String = "#000000"
    var generateExplosion: Int? = null
    var crosshair: String? = null
    fun init(section: ConfigurationSection?): EmpireGun? {
        section?:return null
        clipSize = section.getInt("EMPIRE_GUN_CLIP_SIZE", 5)
        gunDamage = section.getDouble("EMPIRE_GUN_DAMAGE", 5.0)
        gunLength = section.getInt("EMPIRE_GUN_LENGTH", 5)
        gunCooldown = section.getDouble("EMPIRE_GUN_COOLDOWN", 0.5)
        gunRecoil = section.getDouble("EMPIRE_GUN_RECOIL", 1.0)
        gunBulletWeight = section.getDouble("EMPIRE_GUN_BULLET_WEIGHT", 0.8)
        bulletColor = section.getString("EMPIRE_GUN_BULLET_COLOR", "#000000") ?: return null
        reloadBy = section.getString("RELOAD_BY") ?: return null

        noAmmoSound = section.getString("NO_AMMO_SOUND") ?: return null
        shootSound = section.getString("SHOOT_SOUND") ?: return null
        reloadSound = section.getString("RELOAD_SOUND") ?: return null
        generateExplosion = section.getInt("GENERATE_EXPLOSION")
        crosshair = section.getString("EMPIRE_GUN_AIM")

        return this
    }
}

data class Event(
    val eventNames: List<String>,
    val commands: List<Command>,
    val potionEffectsAdd: List<PotionEffect>,
    val potionEffectsRemove: List<PotionEffectType>,
    val particlePlay: ParticleBuilder?,
    val soundsPlay: Sound?,
    val cooldown: Int
) {
    constructor(section: ConfigurationSection) : this(
        section.getStringList("events_names"),
        getCommands(section.getConfigurationSection("play_command")),
        getPotionEffectAdd(section.getConfigurationSection("potion_effect")),
        getPotionEffectRemove(section.getStringList("potion_effect_remove")),
        getParticles(section.getConfigurationSection("play_particle")),
        getSounds(section.getConfigurationSection("play_sound")),
        section.getInt("cooldown", -1),
    )
}

data class Sound(
    val name: String,
    val volume: Double,
    val pitch: Double
)

data class Command(
    val command: String,
    val asConsole: Boolean
)