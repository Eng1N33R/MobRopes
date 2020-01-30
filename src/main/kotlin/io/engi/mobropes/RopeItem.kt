package io.engi.mobropes

import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.ListTag
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

class RopeItem(settings: Settings?) : Item(settings) {
    private var onDurabilityTick: (stack: ItemStack, entity: Entity)->Unit = { _: ItemStack, _: Entity -> }
    private var isValidTarget: (entity: LivingEntity)->Boolean = { true }
    private var ticking = false
    private var damageTicks = 100
    private var restoreTicks = 50

    fun withTicks(damageTicks: Int, restoreTicks: Int): RopeItem {
        this.ticking = true
        this.damageTicks = damageTicks
        this.restoreTicks = restoreTicks
        return this
    }

    fun withTickListener(onDamageTick: (stack: ItemStack, entity: Entity)->Unit): RopeItem {
        this.onDurabilityTick = onDamageTick
        return this
    }

    fun withTargetChecker(isValidTarget: (entity: LivingEntity)->Boolean): RopeItem {
        this.isValidTarget = isValidTarget
        return this
    }

    // Convenience functions

    private fun doubleListOf(vararg values: Double): ListTag {
        val tag = ListTag()
        tag.addAll(values.map(DoubleTag::of))
        return tag
    }

    private fun createEntityFromTag(tag: CompoundTag?, world: World, pos: ListTag?): Boolean {
        tag?.put("Pos", pos)
        tag?.put("Motion", doubleListOf(0.0, 0.0, 0.0))
        return EntityType.getEntityFromTag(tag, world).map(world::spawnEntity).isPresent
    }

    private fun saveEntityToStack(entity: LivingEntity?, stack: ItemStack?): Boolean {
        val entityTag = CompoundTag()
        if (entity?.saveSelfToTag(entityTag) != true) return false
        stack?.orCreateTag?.put("CaughtEntity", entityTag)
        entity.remove()
        return true
    }

    private var durationTicks = 0
    private fun tickDamage(stack: ItemStack, carrier: Entity): Boolean {
        if (carrier.world?.isClient != false) return false
        durationTicks++
        if (stack.tag?.contains("CaughtEntity") != true) {
            if (stack.damage < 1) return false
            if (durationTicks >= restoreTicks) {
                durationTicks = 0
                if (carrier is PlayerEntity && carrier.itemCooldownManager.isCoolingDown(this)) {
                    return false
                }
                stack.damage--
                return true
            }
        } else {
            if (stack.damage >= stack.maxDamage) return false
            if (durationTicks >= damageTicks) {
                durationTicks = 0
                if (stack.damage < stack.maxDamage - 1) {
                    stack.damage++
                }
                if (stack.damage >= stack.maxDamage - 1) {
                    createEntityFromTag(stack.getSubTag("CaughtEntity"), carrier.world,
                            doubleListOf(carrier.x, carrier.y + 0.5, carrier.z)
                    )
                    stack.removeSubTag("CaughtEntity")
                    if (carrier is PlayerEntity) {
                        carrier.itemCooldownManager[this] = 200
                    }
                }
                return true
            }
        }
        return false
    }

    // Catch entity
    override fun useOnEntity(stack: ItemStack?, user: PlayerEntity?, entity: LivingEntity?, hand: Hand?): Boolean {
        if (user?.world?.isClient != false) return false
        if (entity == null || stack == null || stack.tag?.contains("CaughtEntity") == true || entity is PlayerEntity)
            return false
        if (stack.damage >= stack.maxDamage - 1)
            return false
        if (isValidTarget(entity) && saveEntityToStack(entity, stack)) {
            user.setStackInHand(hand, stack)
            user.itemCooldownManager[this] = 10
        }
        return false
    }

    // Release entity
    override fun useOnBlock(context: ItemUsageContext?): ActionResult {
        if (context?.world?.isClient != false) return ActionResult.PASS
        if (context.stack.tag?.contains("CaughtEntity") != true) return ActionResult.FAIL

        val pos = context.blockPos.offset(context.side)
        val entityTag = context.stack.getSubTag("CaughtEntity")
        context.stack.removeSubTag("CaughtEntity")

        createEntityFromTag(entityTag, context.world,
            doubleListOf(pos.x.toDouble() + 0.5, pos.y.toDouble(), pos.z.toDouble() + 0.5))

        EntityType.getEntityFromTag(entityTag, context.world)
            .map(context.world::spawnEntity)
        context.player?.setStackInHand(context.hand, context.stack)
        context.player!!.itemCooldownManager[this] = 30
        return ActionResult.SUCCESS
    }

    // Shift+Use to release at current position
    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack> {
        val held = user?.getStackInHand(hand)
        if (world?.isClient != false) return TypedActionResult.pass(held)
        if (user?.isSneaking == true && held?.hasTag() == true && held.tag?.contains("CaughtEntity") == true) {
            createEntityFromTag(held.getSubTag("CaughtEntity"), world,
                    doubleListOf(user.x, user.y + 0.5, user.z)
            )
            held.removeSubTag("CaughtEntity")
            user.setStackInHand(hand, held)
            return TypedActionResult.success(held)
        }
        return TypedActionResult.pass(held)
    }

    // Tick item damage and listener
    override fun inventoryTick(stack: ItemStack?, world: World?, entity: Entity?, slot: Int, selected: Boolean) {
        if (!ticking) return
        if (tickDamage(stack!!, entity!!)) {
            onDurabilityTick(stack, entity)
        }
    }

    // Item overrides
    override fun appendTooltip(
            stack: ItemStack?,
            world: World?,
            tooltip: MutableList<Text>?,
            context: TooltipContext?
    ) {
        if (stack?.tag?.contains("CaughtEntity") != true) return
        EntityType.fromTag(stack.getSubTag("CaughtEntity"))
                .map { t -> tooltip?.add(t.name.formatted(Formatting.GRAY)) }
    }

    override fun hasEnchantmentGlint(stack: ItemStack?): Boolean {
        return stack?.tag?.contains("CaughtEntity") == true
    }

    override fun shouldSyncTagToClient(): Boolean {
        return true
    }

    override fun isDamageable(): Boolean {
        return true
    }
}