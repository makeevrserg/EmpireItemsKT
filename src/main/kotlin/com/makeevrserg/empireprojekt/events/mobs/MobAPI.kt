package com.makeevrserg.empireprojekt.events.mobs

import org.bukkit.Location
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.random.Random

class MobAPI {

    fun getLivingEntity(e: Entity): LivingEntity? {
        if (e !is LivingEntity)
            return null
        return e as LivingEntity
    }

    private fun setNameTag(e: Entity, tag: String) {
        (e as CraftEntity).handle.addScoreboardTag(tag)
    }

    private fun getNameTag(e: Entity): MutableSet<String>? {
        return (e as CraftEntity).handle.scoreboardTags
    }

    fun getEmpireMob(e: Entity): EmpireMobsManager.EmpireMob? {
        for (tag in (e as CraftEntity).handle.scoreboardTags) {
            val mob = EmpireMobsManager.empireMobs[tag]
            if (mob != null)
                return mob
        }
        return null
    }

    fun changeMobState(
        entity: LivingEntity,
        empireMob: EmpireMobsManager.EmpireMob,
        state: EmpireMobsManager.EmpireMob.STATE
    ) {
        fun setAnim(stand: ArmorStand?, entity: LivingEntity, item: ItemStack) {

            if (stand != null)
                stand.equipment!!.helmet = item
            entity.equipment!!.helmet = item
        }

        val aStand: ArmorStand? = if (empireMob.useArmorStand) getEntityArmorStand(entity, empireMob) else null
        when (state) {
            EmpireMobsManager.EmpireMob.STATE.IDLE ->
                setAnim(aStand, entity, empireMob.idleAnimation)
            EmpireMobsManager.EmpireMob.STATE.WALK ->
                setAnim(aStand, entity, empireMob.walkAnimation)

            EmpireMobsManager.EmpireMob.STATE.ATTACK ->
                setAnim(aStand, entity, empireMob.attackAnimation)

        }

    }

    private fun isItemIsMobAnim(itemStack: ItemStack, mob: EmpireMobsManager.EmpireMob): Boolean {
        if (listOf(mob.idleAnimation, mob.walkAnimation, mob.attackAnimation).contains(itemStack))
            return true
        return false

    }

    private fun getEntityArmorStand(e: Entity, empireMob: EmpireMobsManager.EmpireMob): ArmorStand? {
        var aStand: ArmorStand? = null
        for (passenger in e.passengers)
            if (passenger is ArmorStand)
                if (isItemIsMobAnim(passenger.equipment?.helmet ?: continue, empireMob))
                    aStand = passenger
        return aStand
    }

    private fun getMobToSpawn(
        list: List<EmpireMobsManager.EmpireMob>,
        entity: Entity
    ): MutableList<EmpireMobsManager.EmpireMob> {
        val mobs = mutableListOf<EmpireMobsManager.EmpireMob>()
        for (mob in list)
            if (mob.replaceMobSpawn[entity.type]?.chance ?: continue > Random.nextDouble(100.0))
                mobs.add(mob)
        return mobs
    }

    fun getEmpireMobByEntity(e: Entity): EmpireMobsManager.EmpireMob? {
        val entity = getLivingEntity(e) ?: return null
        val empireMobs = EmpireMobsManager.empireMobsByEntitySpawn[entity.type] ?: return null
        val mobsToSpawn = getMobToSpawn(empireMobs, entity)
        if (mobsToSpawn.isEmpty())
            return null
        return mobsToSpawn[Random.nextInt(mobsToSpawn.size)]
    }

    public fun spawnMob(location: Location, empireMob: EmpireMobsManager.EmpireMob) {



        EmpireMobsManager.spawnList.add(location)
        val ent = (location.world!!.spawnEntity(location, empireMob.ai))
        ent.teleport(location)
        val entity = ent as LivingEntity

        for (attr in empireMob.attributes)
            entity.getAttribute(attr.attribute)?.baseValue = Random.nextDouble(attr.min, attr.max + 0.0001)


        setNameTag(entity, empireMob.id)
        entity.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 999999999, 99999999, false, false, false))

        if (empireMob.useArmorStand) {
            val aStand = entity.world.spawnEntity(entity.location, EntityType.ARMOR_STAND) as ArmorStand
            aStand.isInvisible = true
            aStand.isInvulnerable = true
            aStand.equipment!!.helmet = empireMob.idleAnimation
            if (empireMob.smallArmorStand)
                aStand.isSmall = true
            entity.addPassenger(aStand)
        } else {
            entity.equipment?.clear() ?: return
            entity.equipment?.helmetDropChance = 0.0f
            entity.equipment?.helmet = empireMob.idleAnimation

        }
        entity.isSilent = true
    }
}