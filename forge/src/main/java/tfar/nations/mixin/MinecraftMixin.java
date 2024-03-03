package tfar.nations.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfar.nations.client.NationsClient;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {

    @Inject(method = "shouldEntityAppearGlowing",at = @At("RETURN"),cancellable = true)
    private void forceGlow(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (pEntity.getTeamColor() == ChatFormatting.GREEN.getColor() && NationsClient.glow) {
            cir.setReturnValue(true);
        }
    }
}