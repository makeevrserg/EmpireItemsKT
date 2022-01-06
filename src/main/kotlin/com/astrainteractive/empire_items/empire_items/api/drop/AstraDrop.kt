package com.astrainteractive.empire_items.empire_items.api.drop

import com.astrainteractive.astralibs.AstraYamlParser
import com.astrainteractive.astralibs.Logger
import com.astrainteractive.astralibs.catching
import com.astrainteractive.astralibs.getFloat
import com.astrainteractive.empire_items.empire_items.api.utils.getCustomItemsFiles
import org.bukkit.configuration.ConfigurationSection
import kotlin.reflect.KProperty1


data class AstraDrop(
    val dropFrom: String,
    val id: String,
    val minAmount: Int=0,
    val maxAmount: Int,
    val chance: Double
) {


    companion object {
        fun getDrops(): List<AstraDrop> = getCustomItemsFiles()?.flatMap { fileManager ->
            getMapDrop(
                fileManager.getConfig().getConfigurationSection("loot") ?: return@flatMap emptyList<AstraDrop>()
            )
        } ?: listOf()


        private fun getMapDrop(s: ConfigurationSection?) = s?.getKeys(false)?.flatMap { dropFrom ->
            val section = s.getConfigurationSection(dropFrom)
            val drops = section?.getKeys(false)?.mapNotNull { itemId ->
                getSingleDrop(dropFrom, itemId, section.getConfigurationSection(itemId))
            } ?: listOf()
            drops
        } ?: listOf()

        private fun getSingleDrop(_dropFrom: String, _itemId: String, s: ConfigurationSection?): AstraDrop? {
            val parser = AstraYamlParser.parser
            val res = parser.configurationSectionToClass<AstraDrop>(s ?: return null) ?: return null
            val id = parser.fixNull(res.id, _itemId)
            val dropFrom = parser.fixNull(res.dropFrom, _dropFrom)
            return AstraDrop(
                dropFrom = dropFrom,
                id = id,
                minAmount = res.minAmount,
                maxAmount = res.maxAmount,
                chance = res.chance
            )
        }
    }
}
