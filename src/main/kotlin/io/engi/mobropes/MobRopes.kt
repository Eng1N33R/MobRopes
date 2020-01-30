package io.engi.mobropes

import net.minecraft.entity.boss.dragon.EnderDragonEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.AmbientEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.mob.WaterCreatureEntity
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

const val MODID = "mobropes"

@Suppress("unused")
class MobRopes {
    fun init() {
        Registry.register(
                Registry.ITEM,
                Identifier(MODID, "golden_rope"),
                RopeItem(Item.Settings().group(ItemGroup.TOOLS).maxDamage(20))
                        .withTicks(100, 50)
                        .withTargetChecker {
                            entity -> entity is AmbientEntity
                                || entity is AnimalEntity
                        }
        )
        Registry.register(
                Registry.ITEM,
                Identifier(MODID, "water_rope"),
                RopeItem(Item.Settings().group(ItemGroup.TOOLS).maxDamage(20))
                        .withTicks(200, 100)
                        .withTargetChecker { entity -> entity is WaterCreatureEntity }
        )
        Registry.register(
                Registry.ITEM,
                Identifier(MODID, "diamond_rope"),
                RopeItem(Item.Settings().group(ItemGroup.TOOLS).maxCount(1))
                        .withTargetChecker {
                            entity -> entity is AmbientEntity
                                || entity is AnimalEntity
                                || entity is WaterCreatureEntity
                        }
        )
        Registry.register(
                Registry.ITEM,
                Identifier(MODID, "emerald_rope"),
                RopeItem(Item.Settings().group(ItemGroup.TOOLS).maxDamage(20))
                        .withTicks(250, 125)
                        .withTargetChecker { entity -> entity is VillagerEntity }
        )
        Registry.register(
                Registry.ITEM,
                Identifier(MODID, "hostile_rope"),
                RopeItem(Item.Settings().group(ItemGroup.TOOLS).maxDamage(20))
                        .withTicks(150, 75)
                        .withTargetChecker { entity -> entity is Monster && entity !is EnderDragonEntity }
                        .withTickListener { _, entity -> entity.damage(DamageSource.GENERIC, 1.toFloat()) }
        )
        Registry.register(
                Registry.ITEM,
                Identifier(MODID, "creative_rope"),
                RopeItem(Item.Settings().group(ItemGroup.TOOLS).maxCount(1))
        )
    }
}
