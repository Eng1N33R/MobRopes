package io.engi.mobropes.mixin;

import io.engi.mobropes.RopeItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
    @Inject(at = @At("HEAD"), method = "interact", cancellable = true)
    public void interact(Entity target, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!target.world.isClient) {
            PlayerEntity user = (PlayerEntity) (Object) this;
            ItemStack held = user.getStackInHand(hand);
            if (!user.isSpectator() && held.getItem() instanceof RopeItem && target instanceof LivingEntity) {
                held.getItem().useOnEntity(held, user, (LivingEntity) target, hand);
                cir.setReturnValue(ActionResult.PASS);
            }
        }
    }
}
