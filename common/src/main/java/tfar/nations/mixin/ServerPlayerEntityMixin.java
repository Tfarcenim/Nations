package tfar.nations.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfar.nations.sgui.impl.PlayerExtensions;
import tfar.nations.sgui.virtual.SguiScreenHandlerFactory;
import tfar.nations.sgui.virtual.VirtualScreenHandlerInterface;

import java.util.OptionalInt;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Player implements PlayerExtensions {

    @Unique
    private boolean sgui_ignoreNext = false;

    public ServerPlayerEntityMixin(Level world, BlockPos pos, float yaw, GameProfile gameProfile, @Nullable ProfilePublicKey publicKey) {
        super(world, pos, yaw, gameProfile, publicKey);
    }

    @Inject(method = "openMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;closeContainer()V", shift = At.Shift.BEFORE), cancellable = true)
    private void sgui_dontForceCloseFor(MenuProvider factory, CallbackInfoReturnable<OptionalInt> cir) {
        if (factory instanceof SguiScreenHandlerFactory<?> sguiScreenHandlerFactory && !sguiScreenHandlerFactory.gui().resetMousePosition()) {
            this.sgui_ignoreNext = true;
        }
    }

    @Inject(method = "closeContainer", at = @At("HEAD"), cancellable = true)
    private void sgui_ignoreClosing(CallbackInfo ci) {
        if (this.sgui_ignoreNext) {
            this.sgui_ignoreNext = false;
            this.closeContainer();
            ci.cancel();
        }
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void sgui_onDeath(DamageSource source, CallbackInfo ci) {
        if (this.containerMenu instanceof VirtualScreenHandlerInterface handler) {
            handler.getGui().close(true);
        }
    }

    @Override
    public void sgui_ignoreNextClose() {
        this.sgui_ignoreNext = true;
    }
}
