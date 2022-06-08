package com.astrainteractive.empire_items.empire_items.events.genericevents

import com.astrainteractive.astralibs.*
import com.astrainteractive.astralibs.async.AsyncHelper.callSyncMethod
import com.astrainteractive.astralibs.events.DSLEvent
import com.astrainteractive.empire_items.api.CraftingApi
import com.astrainteractive.empire_items.api.EmpireItemsAPI
import com.astrainteractive.empire_items.api.EmpireItemsAPI.empireID
import com.astrainteractive.empire_items.api.EmpireItemsAPI.toAstraItemOrItem
import com.destroystokyo.paper.ParticleBuilder
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Furnace
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.FurnaceSmeltEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.Future

class ItemInteractEvent {

    private val cooldown = mutableMapOf<String, Long>()

    fun hasCooldown(player: Player, event: String, _cooldown: Int): Boolean {
        val lastUse = cooldown[player.name + event] ?: 0L
        if (System.currentTimeMillis() - lastUse < _cooldown)
            return true
        cooldown[player.name + event] = System.currentTimeMillis()
        return false
    }

    fun executeEvent(item: ItemStack, player: Player, event: String): Boolean {
        val id = item.empireID
        val itemInfo = EmpireItemsAPI.itemYamlFilesByID[id] ?: return false
        val interact = itemInfo.interact ?: return false
        var executed = false
        interact.forEach { (_, it) ->
            executed = true
            if (!it.eventList.contains(event))
                return@forEach
            if (hasCooldown(player, event, it.cooldown ?: 0))
                return@forEach
            it.playCommand.values.syncForEach { cmd ->
                if (cmd.asConsole)
                    AstraLibs.instance.server.dispatchCommand(AstraLibs.instance.server.consoleSender, cmd.command)
                else player.performCommand(cmd.command)
            }
            it.playParticle.values.syncForEach playParticle@{ particle ->
                ParticleBuilder(valueOfOrNull<Particle>(particle.name) ?: return@playParticle)
                    .count(particle.count)
                    .extra(particle.time)
                    .location(player.location.add(0.0, 1.5, 0.0)).spawn()
            }
            it.playPotionEffect.values.syncForEach playPotion@{ effect ->
                effect.play(player)
            }
            it.removePotionEffect.syncForEach removeEffect@{ effect ->
                PotionEffectType.getByName(effect)?.let { player.removePotionEffect(it) }

            }
            it.playSound.values?.syncForEach { sound ->
                sound.play(player.location)
            }
        }
        return executed
    }

    private inline fun <T> Iterable<T>.syncForEach(crossinline action: (T) -> Unit): Future<Unit>? =
        callSyncMethod {
            for (element in this) action(element)
        }


    val onClick = DSLEvent.event(PlayerInteractEvent::class.java) { e ->
        if (e.hand == EquipmentSlot.HAND)
            executeEvent(item = e.player.inventory.itemInMainHand, player = e.player, event = e.action.name)
        if (e.hand == EquipmentSlot.OFF_HAND)
            executeEvent(item = e.player.inventory.itemInOffHand, player = e.player, event = e.action.name)
    }

    val onDrink = DSLEvent.event(PlayerItemConsumeEvent::class.java) { e ->
        val executed = executeEvent(item = e.player.inventory.itemInMainHand, player = e.player, event = e.eventName)
        if (executed)
            e.replacement = ItemStack(Material.AIR)
    }

    val onFurnaceEnded = DSLEvent.event(FurnaceSmeltEvent::class.java) { e ->
        val id = e.source.empireID ?: return@event
        val returnId = CraftingApi.getFurnaceByInputId(id)?.returns?.toAstraItemOrItem() ?: return@event
        if (e.block.state !is Furnace)
            return@event
        val furnace = e.block.state as Furnace
        furnace.inventory.smelting = returnId
    }
    val onEntityDamage = DSLEvent.event(EntityDamageEvent::class.java) { e ->
        if (e.entity !is Player)
            return@event
        executeEvent(
            item = (e.entity as Player).inventory.itemInMainHand,
            player = (e.entity as Player),
            event = e.eventName
        )
    }

    val onPlayerJoin = DSLEvent.event(PlayerJoinEvent::class.java) { e ->
        cooldown.remove(e.player.name)
    }


    val onPlayerQuit = DSLEvent.event(PlayerQuitEvent::class.java) { e ->
        cooldown.remove(e.player.name)
    }
}