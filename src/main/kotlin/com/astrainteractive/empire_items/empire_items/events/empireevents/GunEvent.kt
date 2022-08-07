package com.astrainteractive.empire_items.empire_items.events.empireevents

import com.astrainteractive.astralibs.AstraLibs
import com.astrainteractive.astralibs.async.AsyncHelper
import com.astrainteractive.astralibs.events.DSLEvent
import com.astrainteractive.astralibs.utils.catching
import com.astrainteractive.astralibs.utils.valueOfOrNull
import com.astrainteractive.empire_items.api.EmpireItemsAPI
import com.astrainteractive.empire_items.api.EmpireItemsAPI.empireID
import com.astrainteractive.empire_items.api.EmpireItemsAPI.toAstraItemOrItem
import com.astrainteractive.empire_items.api.utils.BukkitConstants
import com.astrainteractive.empire_items.api.utils.getPersistentData
import com.astrainteractive.empire_items.api.utils.setPersistentDataType
import com.astrainteractive.empire_items.empire_items.util.protection.KProtectionLib
import com.astrainteractive.empire_items.api.models.yml_item.Gun
import com.astrainteractive.empire_items.api.models.yml_item.Interact
import com.astrainteractive.empire_items.api.models.yml_item.YmlItem
import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.destroystokyo.paper.ParticleBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

class GunEvent {

    private var protocolManager: ProtocolManager? = null

    init {
        if (AstraLibs.instance.server.pluginManager.isPluginEnabled("protocollib"))
            protocolManager = ProtocolLibrary.getProtocolManager()
    }


    private val lastShootMap: MutableMap<String, Long> = mutableMapOf()

    private fun canShoot(player: Player, gun: Gun): Boolean {
        val lastShoot = lastShootMap[player.name]
        return when {
            System.currentTimeMillis().minus(lastShoot ?: 0) >= gun.cooldown ?: 0 -> {
                lastShootMap[player.name] = System.currentTimeMillis()
                true
            }
            lastShoot == null -> {
                lastShootMap[player.name] = System.currentTimeMillis()
                true
            }
            else -> false
        }
    }

    private fun setItemDamage(item: ItemStack, maxClipSize: Int?) {
        maxClipSize ?: return
        val itemMeta = item.itemMeta
        val currentClipSize = itemMeta.getPersistentData(BukkitConstants.CLIP_SIZE) ?: return
        val maxDur = item.type.maxDurability.toInt()
        (itemMeta as Damageable).damage = maxDur - (maxDur * currentClipSize / maxClipSize)
        if (itemMeta.damage == 0)
            itemMeta.damage = 1
        item.itemMeta = itemMeta
    }

    private fun reloadGun(player: Player, itemStack: ItemStack, gun: Gun) {
        val itemMeta = itemStack.itemMeta
        val currentClipSize = itemMeta.getPersistentData(BukkitConstants.CLIP_SIZE) ?: return
        if (currentClipSize == gun.clipSize) {
            player.world.playSound(player.location, gun.reloadSound ?: "", 1.0f, 1.0f)
            return
        }
        val reloadBy = gun.reload.toAstraItemOrItem() ?: return
        if (player.inventory.containsAtLeast(reloadBy, 1)) {
            player.inventory.removeItem(reloadBy)
            player.world.playSound(player.location, gun.reloadSound ?: "", 1.0f, 1.0f)
            itemMeta.setPersistentDataType(BukkitConstants.CLIP_SIZE, gun.clipSize)
        }
        itemStack.itemMeta = itemMeta
        setItemDamage(itemStack, gun.clipSize)

    }

    private fun setRecoil(player: Player, recoil: Double) {
        fun setProtocolPitch(p: Player, recoil: Double) {
            val yawPacket = protocolManager?.createPacket(PacketType.Play.Server.POSITION, false) ?: return
            yawPacket.modifier.writeDefaults()
            yawPacket.doubles.write(0, p.location.x)
            yawPacket.doubles.write(1, p.location.y)
            yawPacket.doubles.write(2, p.location.z)
            yawPacket.float.write(0, p.location.yaw)
            yawPacket.float.write(1, p.location.pitch - recoil.toFloat())
            protocolManager?.sendServerPacket(p, yawPacket) ?: return
        }
        if (recoil == 0.0)
            return

        if (player.location.block.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).type == Material.AIR)
            return
        if (AstraLibs.instance.server.pluginManager.isPluginEnabled("protocollib")) {
            setProtocolPitch(player, recoil)
            return
        }

        val loc = player.location.clone()
        loc.pitch -= recoil.toFloat()
        player.teleport(loc)
    }

    fun <T> awaitSync(block: () -> T): T? = AsyncHelper.callSyncMethod {
        block()
    }?.get()

    //#FFFFFF
    private fun rgbToColor(color: String): Color =
        catching { Color.fromRGB(Integer.decode(color.replace("#", "0x"))) } ?: Color.BLACK

    val playerInteractEvent = DSLEvent.event(PlayerInteractEvent::class.java) { e ->
        AsyncHelper.launch(Dispatchers.IO) event@{
            val itemStack = e.item ?: return@event
            val id = itemStack.empireID
            val gunInfo = EmpireItemsAPI.itemYamlFilesByID[id]?.gun ?: return@event
            val player = e.player
            val action = e.action
            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                awaitSync {
                    reloadGun(player, itemStack, gunInfo)
                }
                return@event
            }
            val currentClipSize = itemStack.itemMeta.getPersistentData(BukkitConstants.CLIP_SIZE)
            if (currentClipSize == 0) {
                awaitSync {
                    player.world.playSound(player.location, gunInfo.noAmmoSound ?: "", 1.0f, 1.0f)
                }
                return@event
            }

            if (!canShoot(player, gunInfo))
                return@event
            Interact.PlaySound(gunInfo.shootSound).play(player.location)
            var itemMeta = itemStack.itemMeta
            if (currentClipSize != null)
                awaitSync { itemMeta.setPersistentDataType(BukkitConstants.CLIP_SIZE, currentClipSize.minus(1)) }
            itemStack.itemMeta = itemMeta
            awaitSync { setItemDamage(itemStack, gunInfo.clipSize) }
            var l = player.location.add(0.0, 1.3, 0.0)
            if (player.isSneaking)
                l = l.add(0.0, -0.2, 0.0)

            if (gunInfo.recoil != null)
                awaitSync { setRecoil(player, gunInfo.recoil) }

            val r = if (player.isSneaking) (gunInfo.radiusSneak ?: gunInfo.radius * 2) else gunInfo.radius


            for (i in 0 until gunInfo.bulletTrace) {
                val particle = valueOfOrNull<Particle>(gunInfo.particle ?: "") ?: Particle.REDSTONE
                var builder = ParticleBuilder(particle)
                    .count(20)
                    .force(true)
                    .extra(0.06)
                    .data(null)
                if (particle == Particle.REDSTONE)
                    builder = builder.color(rgbToColor(gunInfo.color ?: "#000000"))
                val clonedLocation1 = l.clone()
                AsyncHelper.callSyncMethod {
                    val l = clonedLocation1
                    builder
                        .location(l.world ?: return@callSyncMethod, l.x, l.y, l.z)
                        .spawn()
                }
                l =
                    l.add(
                        l.direction.x,
                        l.direction.y - i / (gunInfo.bulletTrace * (gunInfo.bulletWeight ?: 1.0)),
                        l.direction.z
                    )

                if (!l.block.isPassable) {
                    gunInfo.advanced?.onHit?.ignite?.let {
                        val l = l.clone()
                        AsyncHelper.callSyncMethod {
                            MolotovEvent.Igniter(l.block.getRelative(BlockFace.UP), it, null, particle = false)
                        }
                    }
                    break
                }
                val clonedL = l.clone()
                AsyncHelper.callSyncMethod {
                    val l = clonedL
                    for (ent: Entity in getEntityByLocation(l, r)) {
                        if (ent is LivingEntity && ent != player) {
                            gunInfo.damage?.let {
                                var damage = (1.0 - i / gunInfo.bulletTrace) * it
                                gunInfo.advanced?.onHit?.let { onHit ->
                                    onHit.playPotionEffect?.forEach {
                                        it.value.play(ent)
                                    }
                                    onHit.fireTicks?.let { ent.fireTicks = it }
                                }
                                gunInfo.advanced?.armorPenetration?.let {

                                    ent.equipment?.let { eq ->
                                        listOf(eq.helmet, eq.chestplate, eq.leggings, eq.boots).forEach { armor ->
                                            val id = armor?.empireID ?: armor?.type?.name ?: return@forEach
                                            val multiplier = it[id] ?: return@forEach
                                            damage *= multiplier
                                        }
                                    }
                                }
                                ent.damage(damage, player)

                            }
                        }
                    }
                }

            }
            if (gunInfo.explosion != null && KProtectionLib.canExplode(null, l))
                awaitSync { GrenadeEvent.generateExplosion(l, gunInfo.explosion.toDouble()) }
        }

    }


    private fun getEntityByLocation(loc: Location, r: Double): MutableCollection<Entity> {
        return loc.world.getNearbyEntities(loc,r,r,r)
        val entities: MutableList<Entity> = mutableListOf()
        loc.world ?: return mutableListOf()
        for (e in loc.world!!.entities)
            if (e.location.distanceSquared(loc) <= r)
                entities.add(e)
        return entities
    }
}
