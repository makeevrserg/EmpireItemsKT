package com.makeevrserg.empireprojekt.events.empireevents

import com.makeevrserg.empireprojekt.EmpirePlugin
import com.makeevrserg.empireprojekt.EmpirePlugin.Companion.instance
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.Flags
import com.sk89q.worldguard.protection.regions.RegionQuery
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.persistence.PersistentDataType

class Molotov : Listener {

    init {
        instance.server.pluginManager.registerEvents(this, instance)
    }



    @EventHandler
    fun onProjectileHit(e: ProjectileHitEvent) {
        if (e.entity.shooter !is Player) return
        val player = e.entity.shooter as Player
        println("Player ${player.name} threw molotov at blockLocation=${e.hitBlock?.location} playerLocation=${player.location}")
        val itemStack = player.inventory.itemInMainHand
        val meta = itemStack.itemMeta ?: return
        val molotovPower =
            meta.persistentDataContainer.get(EmpirePlugin.empireConstants.MOLOTOV, PersistentDataType.DOUBLE)
                ?: return


        Igniter(instance,e.hitBlock ?: return, molotovPower.toInt(), player)
    }

    companion object{
        fun allowFire(plugin: EmpirePlugin, location: Location): Boolean {
            if (plugin.server.pluginManager.getPlugin("WorldGuard") != null) {
                val query: RegionQuery = WorldGuard.getInstance().platform.regionContainer.createQuery()
                val loc: com.sk89q.worldedit.util.Location = BukkitAdapter.adapt(location)
                return (query.testState(loc, null, Flags.FIRE_SPREAD))
            }
            return true
        }
    }

    class Igniter(val plugin: EmpirePlugin, block: Block, radius: Int, player: Player) {
        private val listLocations: MutableList<Location> = mutableListOf()

        init {
            block.location.world?.spawnParticle(Particle.SMOKE_LARGE, block.location, 300, 0.0, 0.0, 0.0, 0.2)
            setFire(block, radius, player)
        }


        private fun checkOnlyAir(block: Block): Boolean {
            return (block.getRelative(BlockFace.DOWN).type == Material.AIR &&
                    block.getRelative(BlockFace.UP).type == Material.AIR &&
                    block.getRelative(BlockFace.EAST).type == Material.AIR &&
                    block.getRelative(BlockFace.WEST).type == Material.AIR &&
                    block.getRelative(BlockFace.NORTH).type == Material.AIR &&
                    block.getRelative(BlockFace.SOUTH).type == Material.AIR
                    )
        }

        private fun checkOnlyBlock(block: Block): Boolean {
            return (block.getRelative(BlockFace.DOWN).type != Material.AIR &&
                    block.getRelative(BlockFace.UP).type != Material.AIR &&
                    block.getRelative(BlockFace.EAST).type != Material.AIR &&
                    block.getRelative(BlockFace.WEST).type != Material.AIR &&
                    block.getRelative(BlockFace.NORTH).type != Material.AIR &&
                    block.getRelative(BlockFace.SOUTH).type != Material.AIR
                    )
        }

        private fun setFire(block: Block, radius: Int, player: Player) {
            if (!allowFire(plugin,block.location))
                return
            if (radius == 0)
                return
            if (checkOnlyAir(block))
                return
            if (checkOnlyBlock(block))
                return

            if (block.location in listLocations)
                return
            else
                listLocations.add(block.location)

            if (block.type == Material.AIR)
                block.type = Material.FIRE


            for (blockFace in BlockFace.values())
                setFire(block.getRelative(blockFace), radius - 1, player)


        }

    }


    fun onDisable() {
        ProjectileHitEvent.getHandlerList().unregister(this)
    }
}
