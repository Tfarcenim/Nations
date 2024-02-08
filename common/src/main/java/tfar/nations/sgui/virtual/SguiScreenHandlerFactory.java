package tfar.nations.sgui.virtual;


import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import tfar.nations.sgui.api.gui.GuiInterface;
import tfar.nations.sgui.api.gui.SlotGuiInterface;
import tfar.nations.sgui.virtual.inventory.VirtualScreenHandler;

public record SguiScreenHandlerFactory<T extends GuiInterface>(T gui, MenuConstructor factory) implements MenuProvider {

    @Override
    public Component getDisplayName() {
        Component text = this.gui.getTitle();
        if (text == null) {
            text = Component.empty();
        }
        return text;
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return factory.createMenu(syncId, playerInventory, player);
    }

    public static <T extends SlotGuiInterface> SguiScreenHandlerFactory<T> ofDefault(T gui) {
        return new SguiScreenHandlerFactory<>(gui, ((syncId, inv, player) -> new VirtualScreenHandler(gui.getType(), syncId, gui, player)));
    }
}
