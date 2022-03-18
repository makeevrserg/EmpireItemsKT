package com.astrainteractive.empire_items.api.mobs

import com.astrainteractive.astralibs.async.AsyncHelper
import com.astrainteractive.astralibs.valueOfOrNull
import com.astrainteractive.empire_items.EmpirePlugin
import com.astrainteractive.empire_items.api.mobs.data.*
import com.astrainteractive.empire_items.api.utils.Cooldown
import com.astrainteractive.empire_items.api.utils.Disableable
import com.astrainteractive.empire_items.empire_items.util.*
import com.destroystokyo.paper.ParticleBuilder
import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone
import com.ticxo.modelengine.api.model.ActiveModel
import com.ticxo.modelengine.api.model.ModeledEntity
import kotlinx.coroutines.*
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarFlag
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.math.max

object MobApi : Disableable {

    private var empireMobs: MutableList<EmpireMob> = mutableListOf()
    private var empireMobsById: Map<String, EmpireMob> = mapOf()

    private val _activeMobs: MutableList<CustomEntityInfo> = mutableListOf()
    val activeMobs: List<CustomEntityInfo>
        get() = _activeMobs

    /**
     * @return list of all ids of [EmpireMob]
     */
    fun getEmpireMobsList() = empireMobs.map { it.id }

    private val ignoredSpawn = mutableSetOf<Location>()

    /**
     * Get [EmpireMob] by its id
     * @return [EmpireMob]
     */
    fun getEmpireMob(id: String) = empireMobsById[id]

    fun getModelEngineMobs() = ModelEngineAPI.api.modelManager.modelRegistry.registeredModel.keys

    /**
     * Play animation for selected [ActiveModel]
     */
    fun ActiveModel.playAnimation(state: String) {
        addState(state, 1, 1, 1.0)
    }

    /**
     * Run task later
     */
    fun runLater(time: Long, block: () -> Unit) {
        AsyncHelper.launch {
            delay(time)
            block()
        }
    }


    /**
     * @return [ModeledEntity]
     */
    val Entity.modeledEntity: ModeledEntity?
        get() = ModelEngineAPI.api.modelManager.restoreModeledEntity(this)

    /**
     * @returm [ActiveModel]
     */
    val ModeledEntity.activeModel: ActiveModel?
        get() = this.allActiveModel?.values?.firstOrNull()

    /**
     * @return id of [ModeledEntity]
     */
    val ModeledEntity.modelId: String?
        get() = this.activeModel?.modelId


    /**
     * @param [e] - Naturally spawned entity
     * @return list of models, which can replace current entity
     */
    fun getByNaturalSpawn(e: Entity): List<EmpireMob>? {
        val mobs = MobApi.empireMobs.filter { emob ->
            emob.spawn?.conditions?.firstOrNull { cond ->

                val chance = cond.replace[e.type.name]
                val c1 = calcChance(chance?.toFloat() ?: -1f)
                val c2 = e.location.y > cond.minY && e.location.y < cond.maxY
                val c3 = if (cond.biomes.isNullOrEmpty()) true else cond.biomes.contains(e.location.getBiome().name)
                c1 && c2 && c3
            } != null
        }
        if (mobs.isNullOrEmpty())
            return null
        return mobs
    }

    val bossBars = mutableMapOf<Entity, BossBar>()

    fun bossBarKey(id: Int) = NamespacedKey(EmpirePlugin.instance, id.toString())
    fun createEntityBossBar(e: Entity, id: String, bossBar: MobBossBar) {
        val key = bossBarKey(e.entityId)
        val barColor = valueOfOrNull<BarColor>(bossBar.color) ?: BarColor.RED
        val barStyle = valueOfOrNull<BarStyle>(bossBar.barStyle) ?: BarStyle.SOLID
        val barFlags = bossBar.flags.mapNotNull { valueOfOrNull<BarFlag>(it) }.toTypedArray()
        val bar =
            Bukkit.createBossBar(key, bossBar.name, barColor, barStyle, *barFlags)
        bar.isVisible = true
        bar.progress = 1.0
        bossBars[e] = bar
    }

    fun deleteEntityBossBar(e: Entity) {
        val bar = bossBars.remove(e) ?: return
        bar.isVisible = false
        Bukkit.getOnlinePlayers().forEach { bar.removePlayer(it) }
        bar.removeAll()
    }

    /**
     * Replace entity [e] with [EmpireMob] as Custom ModelEngine entity
     */
    fun replaceEntity(eMob: EmpireMob, e: Entity, naturalSpawn: Boolean = false): CustomEntityInfo? {
        if (naturalSpawn) {
            val loc = e.location.clone()
            spawnMob(eMob, loc)
            return null
        }
        eMob.attributes.forEach {
            val attr = valueOfOrNull<Attribute>(it.attribute) ?: return@forEach
            val amount = it.amount
            (e as LivingEntity).registerAttribute(attr)
            (e as LivingEntity).getAttribute(attr)!!.addModifier(
                AttributeModifier(
                    UUID.randomUUID(),
                    attr.name,
                    amount,
                    AttributeModifier.Operation.ADD_NUMBER
                )
            )
            if (attr == Attribute.GENERIC_MAX_HEALTH)
                (e as LivingEntity).health = amount

        }
        eMob.bossBar?.let { bossBar ->
            createEntityBossBar(e, eMob.id, bossBar)

        }
        (e as LivingEntity).equipment?.clear()

        if (!eMob.canBurn) {
            e.isVisualFire = false
            e.fireTicks = 0
            (e as LivingEntity).equipment?.setHelmet(ItemStack(Material.BARRIER), true)
        }
        eMob.potionEffects.forEach { effect ->
            val potionEffect = PotionEffectType.getByName(effect.effect) ?: return@forEach

            (e as LivingEntity).addPotionEffect(
                PotionEffect(potionEffect, effect.duration, effect.level, false, false, false)
            )
        }
        e.isSilent = true
        val model = ModelEngineAPI.api.modelManager.createActiveModel(eMob.id)
        val modeledEntity = ModelEngineAPI.api.modelManager.createModeledEntity(e)
        modeledEntity.addActiveModel(model)
        modeledEntity.detectPlayers()
        modeledEntity.activeModel?.modelId
        modeledEntity?.isInvisible = true
        val customEntityInfo = CustomEntityInfo(e, eMob, modeledEntity, model)
        _activeMobs.add(customEntityInfo)
        return customEntityInfo
    }


    /**
     * Remove active entity from list
     */
    fun removeActiveEntity(entity: Entity) {
        _activeMobs.removeIf { it.entity == entity }
    }

    /**
     * Get all active entites as List of [CustomEntityInfo]
     */
    fun getActiveEntities() = _activeMobs.toList()
    fun getActiveEntity(e: Entity) = _activeMobs.firstOrNull() { it.entity == e }

    /**
     * Should spawned entity in location [l] be ignored
     * @return true/false
     */
    fun isSpawnIgnored(l: Location): Boolean {
        if (ignoredSpawn.contains(l)) {
            ignoredSpawn.remove(l)
            return true
        }
        return false
    }

    /**
     * Ignore spawn event for location [l]
     */
    private fun ignoreSpawnForLocation(l: Location) {
        ignoredSpawn.add(l)
    }

    /**
     * Spawn an [EmpireMob] in location [l]
     * @return [CustomEntityInfo]
     */
    fun spawnMob(eMob: EmpireMob, l: Location): CustomEntityInfo? {
        ignoreSpawnForLocation(l)
        val mob = l.world.spawnEntity(l, EntityType.fromName(eMob.entity) ?: return null)
        return replaceEntity(eMob, mob)
    }

    private val attackingMobs = mutableSetOf<Int>()
    private fun setAttackAnimationTrack(entity: Entity) = synchronized(MobApi) { attackingMobs.add(entity.entityId) }
    fun stopAttackAnimationTrack(entity: Entity) = synchronized(MobApi) { attackingMobs.remove(entity.entityId) }
    fun isAttackAnimationTracked(entity: Entity) = synchronized(MobApi) { attackingMobs.contains(entity.entityId) }

    /**
     * @return [CustomEntityInfo]
     */
    fun getCustomEntityInfo(e: Entity): CustomEntityInfo? {
        val modeledEntity = e.modeledEntity ?: return null
        val activeModel = modeledEntity.activeModel ?: return null
        val empireMob = getEmpireMob(activeModel.modelId) ?: return null
        return CustomEntityInfo(e, empireMob, modeledEntity, activeModel)
    }

    /**
     * Perform attack for custom entity
     */
    fun performAttack(entityInfo: CustomEntityInfo?, entities: List<Entity>, _damage: Double) {
        entityInfo ?: return
        val modeledEntity = entityInfo.modeledEntity
        val activeModel = entityInfo.activeModel
        val empireMob = entityInfo.empireMob
        val damager = entityInfo.entity
        AsyncHelper.runBackground {
            val frame = activeModel.getState("attack")?.frame
            val animationLength = activeModel.getState("attack")?.animationLength
            if (animationLength == null || frame == null || frame < 0f || frame.toInt() >= animationLength) {
                synchronized(MobApi) {
                    if (isAttackAnimationTracked(damager))
                        return@runBackground
                    setAttackAnimationTrack(damager)
                }
                activeModel.playAnimation("attack")
            } else return@runBackground
            runLater(empireMob.hitDelay.toLong()) {
                entities.forEach { entity ->
                    val distance = damager.location.distance(entity.location)
                    if (distance > (empireMob.hitRange ?: 5))
                        return@forEach
                    val damage = if (empireMob.decreaseDamageByRange)
                        _damage / max(1.0, distance)
                    else _damage

                    empireMob.events["onDamage"]?.let { event ->
                        executeEvent(entity, activeModel, event, "onDamage")
                    }
                    AsyncHelper.callSyncMethod {
                        if ((damager as LivingEntity).health > 0)
                            (entity as LivingEntity).damage(damage, damager)
                    }
                }
            }
        }

    }

    private val particleCooldown: Cooldown<Int> = Cooldown()

    private fun playParticle(e: Entity, model: ActiveModel, bonesInfo: List<BoneInfo>?) {
        bonesInfo?.forEach { boneInfo ->
            if (particleCooldown.hasCooldown(e.entityId, boneInfo.particle.cooldown))
                return
            else particleCooldown.setCooldown(e.entityId)
            var particleBuilder = ParticleBuilder(Particle.valueOf(boneInfo.particle.name))
                .extra(boneInfo.particle.extra)
                .count(boneInfo.particle.amount)
                .force(true)
            particleBuilder = if (boneInfo.particle.color != null)
                particleBuilder.color(boneInfo.particle.color)
            else particleBuilder

            boneInfo.bones.forEach { bones ->
                val l = e.location.clone()
                var modelBone: BlueprintBone? = null
                bones.split(".").forEach bon@{ bone ->
                    modelBone = modelBone?.getBone(bone) ?: model.blueprint.getBone(bone)
                    modelBone?.let {
                        l.add(it.localOffsetX, it.localOffsetY, it.localOffsetZ)
                    }
                }
                particleBuilder.location(l).spawn()
            }
        }

    }


    val eventTimers: Cooldown<String> = Cooldown()

    fun executeEvent(entity: Entity, model: ActiveModel, event: EmpireMobEvent, eventId: String) {
        val hasSoundCooldown = eventTimers.hasCooldown("${eventId}S${entity.hashCode()}", event.sound?.cooldown)
        if (event.sound?.cooldown == null || event.sound.cooldown == 0 || !hasSoundCooldown) {
            eventTimers.setCooldown("${eventId}S${entity.hashCode()}")
            entity.location.playSound(event.sound?.sound)
        }
        playParticle(entity, model, event.bones)
    }


    override suspend fun onEnable() {
        if (Bukkit.getServer().pluginManager.getPlugin("ModelEngine") == null) return

        empireMobs = EmpireMob.getAll().toMutableList()
        empireMobsById = empireMobs.associateBy { it.id }
        AsyncHelper.callSyncMethod {
            val entities = Bukkit.getWorlds().flatMap { world ->
                world.entities.mapNotNull {
                    return@mapNotNull getCustomEntityInfo(it)
                }
            }
            _activeMobs.addAll(entities)
        }
    }

    override suspend fun onDisable() {
        if (Bukkit.getServer().pluginManager.getPlugin("ModelEngine") == null) return
        _activeMobs.forEach {
            it.modeledEntity.removeModel(it.activeModel.modelId)
            it.entity.remove()
        }
        empireMobs.clear()
        _activeMobs.clear()
        bossBars.toMap().forEach { deleteEntityBossBar(it.key) }
    }

}

