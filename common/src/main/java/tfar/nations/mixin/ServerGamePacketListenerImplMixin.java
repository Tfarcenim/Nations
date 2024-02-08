package tfar.nations.mixin;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.nations.sgui.api.ClickType;
import tfar.nations.sgui.api.GuiHelpers;
import tfar.nations.sgui.api.gui.AnvilInputGui;
import tfar.nations.sgui.api.gui.HotbarGui;
import tfar.nations.sgui.api.gui.SignGui;
import tfar.nations.sgui.api.gui.SimpleGui;
import tfar.nations.sgui.virtual.FakeScreenHandler;
import tfar.nations.sgui.virtual.VirtualScreenHandlerInterface;
import tfar.nations.sgui.virtual.book.BookScreenHandler;
import tfar.nations.sgui.virtual.hotbar.HotbarScreenHandler;
import tfar.nations.sgui.virtual.inventory.VirtualScreenHandler;
import tfar.nations.sgui.virtual.merchant.VirtualMerchantScreenHandler;

import java.util.List;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Unique
    private AbstractContainerMenu sgui_previousScreen = null;

    @Shadow
    public ServerPlayer player;

    @Shadow
    public abstract void send(Packet<?> packet);

    @Inject(method = "handleContainerClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V", shift = At.Shift.AFTER), cancellable = true)
    private void sgui_handleGuiClicks(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandler handler) {
            try {
                var gui = handler.getGui();

                int slot = packet.getSlotNum();
                int button = packet.getButtonNum();
                ClickType type = ClickType.toClickType(packet.getClickType(), button, slot);
                boolean ignore = gui.onAnyClick(slot, type, packet.getClickType());
                if (ignore && !handler.getGui().getLockPlayerInventory() && (slot >= handler.getGui().getSize() || slot < 0 || handler.getGui().getSlotRedirect(slot) != null)) {
                    if (type == ClickType.MOUSE_DOUBLE_CLICK || (type.isDragging && type.value == 2)) {
                        GuiHelpers.sendPlayerScreenHandler(this.player);
                    }

                    return;
                }

                boolean allow = gui.click(slot, type, packet.getClickType());
                if (handler.getGui().isOpen()) {
                    if (!allow) {
                        if (slot >= 0 && slot < handler.getGui().getSize()) {
                            this.send(new ClientboundContainerSetSlotPacket(handler.containerId, handler.incrementStateId(), slot, handler.getSlot(slot).getItem()));
                        }
                        GuiHelpers.sendSlotUpdate(this.player, -1, -1, this.player.containerMenu.getCarried(), handler.getStateId());

                        if (type.numKey) {
                            int x = type.value + handler.slots.size() - 10;
                            GuiHelpers.sendSlotUpdate(player, handler.containerId, x, handler.getSlot(x).getItem(), handler.incrementStateId());
                        } else if (type == ClickType.MOUSE_DOUBLE_CLICK || type == ClickType.MOUSE_LEFT_SHIFT || type == ClickType.MOUSE_RIGHT_SHIFT || (type.isDragging && type.value == 2)) {
                            GuiHelpers.sendPlayerScreenHandler(this.player);
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                ci.cancel();
            }

            ci.cancel();
        } else if (this.player.containerMenu instanceof BookScreenHandler) {
            ci.cancel();
        }
    }

    @Inject(method = "handleContainerClick", at = @At("TAIL"))
    private void sgui_resyncGui(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandler) {
            try {
                int slot = packet.getSlotNum();
                int button = packet.getButtonNum();
                ClickType type = ClickType.toClickType(packet.getClickType(), button, slot);

                if (type == ClickType.MOUSE_DOUBLE_CLICK || (type.isDragging && type.value == 2) || type.shift) {
                    GuiHelpers.sendPlayerScreenHandler(this.player);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "handleContainerClose", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;doCloseContainer()V", shift = At.Shift.BEFORE))
    private void sgui_storeScreenHandler(ServerboundContainerClosePacket packet, CallbackInfo info) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface) {
            this.sgui_previousScreen = this.player.containerMenu;
        }
    }

    @Inject(method = "handleContainerClose", at = @At("TAIL"))
    private void sgui_executeClosing(ServerboundContainerClosePacket packet, CallbackInfo info) {
        try {
            if (this.sgui_previousScreen != null) {
                if (this.sgui_previousScreen instanceof VirtualScreenHandlerInterface screenHandler) {
                    screenHandler.getGui().close(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.sgui_previousScreen = null;
    }


    @Inject(method = "handleRenameItem", at = @At("TAIL"))
    private void sgui_catchRenamingWithCustomGui(ServerboundRenameItemPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandler) {
            try {
                VirtualScreenHandler handler = (VirtualScreenHandler) this.player.containerMenu;
                if (handler.getGui() instanceof AnvilInputGui) {
                    ((AnvilInputGui) handler.getGui()).input(packet.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "handlePlaceRecipe", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V", shift = At.Shift.BEFORE), cancellable = true)
    private void sgui_catchRecipeRequests(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandler handler && handler.getGui() instanceof SimpleGui gui) {
            try {
                gui.onCraftRequest(packet.getRecipe(), packet.isShiftDown());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "updateSignText", at = @At("HEAD"), cancellable = true)
    private void sgui_catchSignUpdate(ServerboundSignUpdatePacket packet, List<FilteredText> signText, CallbackInfo ci) {
        try {
            if (this.player.containerMenu instanceof FakeScreenHandler fake && fake.getGui() instanceof SignGui gui) {
                for (int i = 0; i < packet.getLines().length; i++) {
                    gui.setLineInternal(i, Component.literal(packet.getLines()[i]));
                }
                gui.close(true);
                ci.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "handleSelectTrade", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = SAME_THREAD), cancellable = true)
    private void sgui_catchMerchantTradeSelect(ServerboundSelectTradePacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualMerchantScreenHandler merchantScreenHandler) {
            int id = packet.getItem();
            merchantScreenHandler.selectNewTrade(id);
            ci.cancel();
        }
    }

    @Inject(method = "handleSetCarriedItem", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = SAME_THREAD), cancellable = true)
    private void sgui_catchUpdateSelectedSlot(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof HotbarScreenHandler handler) {
            if (!handler.getGui().onSelectedSlotChange(packet.getSlot())) {
                this.send(new ClientboundSetCarriedItemPacket(handler.getGui().getSelectedSlot()));
            }
            ci.cancel();
        }
    }

    @Inject(method = "handleSetCreativeModeSlot", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = SAME_THREAD), cancellable = true)
    private void sgui_cancelCreativeAction(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface) {
            ci.cancel();
        }
    }

    @Inject(method = "handleAnimate", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = SAME_THREAD), cancellable = true)
    private void sgui_clickHandSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof HotbarScreenHandler screenHandler) {
            var gui = screenHandler.getGui();
            if (!gui.onHandSwing()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleUseItem", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = SAME_THREAD), cancellable = true)
    private void sgui_clickWithItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof HotbarScreenHandler screenHandler) {
            var gui = screenHandler.getGui();
            if (screenHandler.slotsOld != null) {
                screenHandler.slotsOld.set(gui.getSelectedSlot() + 36, ItemStack.EMPTY);
                screenHandler.slotsOld.set(45, ItemStack.EMPTY);
            }
            gui.onClickItem();
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = SAME_THREAD), cancellable = true)
    private void sgui_clickOnBlock(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof HotbarScreenHandler screenHandler) {
            var gui = screenHandler.getGui();

            if (!gui.onClickBlock(packet.getHitResult())) {
                var pos = packet.getHitResult().getBlockPos();
                if (screenHandler.slotsOld != null) {
                    screenHandler.slotsOld.set(gui.getSelectedSlot() + 36, ItemStack.EMPTY);
                    screenHandler.slotsOld.set(45, ItemStack.EMPTY);
                }

                this.send(new ClientboundBlockUpdatePacket(pos, this.player.level.getBlockState(pos)));
                pos = pos.relative(packet.getHitResult().getDirection());
                this.send(new ClientboundBlockUpdatePacket(pos, this.player.level.getBlockState(pos)));
                this.send(new ClientboundBlockChangedAckPacket(packet.getSequence()));

                ci.cancel();
            }
        }
    }

    private static final String SAME_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V";

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = SAME_THREAD), cancellable = true)
    private void sgui_onPlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof HotbarScreenHandler screenHandler) {
            var gui = screenHandler.getGui();

            if (!gui.onPlayerAction(packet.getAction(), packet.getDirection())) {
                var pos = packet.getPos();
                if (screenHandler.slotsOld != null) {
                    screenHandler.slotsOld.set(gui.getSelectedSlot() + 36, ItemStack.EMPTY);
                    screenHandler.slotsOld.set(45, ItemStack.EMPTY);
                }
                this.send(new ClientboundBlockUpdatePacket(pos, this.player.level.getBlockState(pos)));
                pos = pos.relative(packet.getDirection());
                this.send(new ClientboundBlockUpdatePacket(pos, this.player.level.getBlockState(pos)));
                this.send(new ClientboundBlockChangedAckPacket(packet.getSequence()));
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleInteract", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = SAME_THREAD), cancellable = true)
    private void sgui_clickOnEntity(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof HotbarScreenHandler screenHandler) {
            var gui = screenHandler.getGui();
            var buf = new FriendlyByteBuf(Unpooled.buffer());
            packet.write(buf);

            int entityId = buf.readVarInt();
            var type = buf.readEnum(HotbarGui.EntityInteraction.class);

            Vec3 interactionPos = null;

            switch (type) {
                case INTERACT:
                    buf.readVarInt();
                    break;
                case INTERACT_AT:
                    interactionPos = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
                    buf.readVarInt();
            }

            var isSneaking = buf.readBoolean();

            if (!gui.onClickEntity(entityId, type, isSneaking, interactionPos)) {
                if (screenHandler.slotsOld != null) {
                    screenHandler.slotsOld.set(gui.getSelectedSlot() + 36, ItemStack.EMPTY);
                    screenHandler.slotsOld.set(45, ItemStack.EMPTY);
                }
                ci.cancel();
            }
        }
    }

    @Inject(method = "*(Lnet/minecraft/network/protocol/game/ServerboundChatPacket;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;getSignedMessage(Lnet/minecraft/network/protocol/game/ServerboundChatPacket;)Lnet/minecraft/network/chat/PlayerChatMessage;"),


            cancellable = true)
    private void sgui_onMessage(ServerboundChatPacket chatMessageC2SPacket, CallbackInfo ci) {
        if (this.player.containerMenu instanceof BookScreenHandler handler) {
            try {
                if (handler.getGui().onCommand(chatMessageC2SPacket.message())) {
                    ci.cancel();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //todo
    @Inject(method = "*(Lnet/minecraft/network/protocol/game/ServerboundChatCommandPacket;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;performChatCommand(Lnet/minecraft/network/protocol/game/ServerboundChatCommandPacket;)V"), cancellable = true)
    private void sgui_onCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof BookScreenHandler handler) {
            try {
                if (handler.getGui().onCommand("/" + packet.command())) {
                    ci.cancel();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
