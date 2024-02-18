package tfar.nations.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class PlayerMixin {

    @Inject(method = "shouldEntityAppearGlowing",at = @At("RETURN"),cancellable = true)
    private void forceGlow(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (pEntity.getTeamColor() == ChatFormatting.GREEN.getColor()) {
            cir.setReturnValue(true);
        }
    }
}