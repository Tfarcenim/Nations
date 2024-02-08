package tfar.nations;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tfar.nations.inventory.NoSlot;

public class YesNoMenu extends AbstractContainerMenu {
    public YesNoMenu(int id, Inventory inventory, Container container) {
        super(MenuType.HOPPER, id);
        checkContainerSize(container, 5);
        container.startOpen(inventory.player);
        int i = 51;

        addSlot(new Slot(container,0,44,20));

        for(int j = 1; j < 4; ++j) {
            this.addSlot(new Slot(container, j, 44 + j * 18, 20));
        }

        addSlot(new NoSlot(container,4,44 + 72,20));


        for(int l = 0; l < 3; ++l) {
            for(int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(inventory, k + l * 9 + 9, 8 + k * 18, l * 18 + 51));
            }
        }

        for(int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(inventory, i1, 8 + i1 * 18, 109));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }
}
