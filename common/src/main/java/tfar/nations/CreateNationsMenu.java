package tfar.nations;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

public class CreateNationsMenu extends ChestMenu {
    public CreateNationsMenu(int id, Inventory $$2, Container container) {
        super(MenuType.GENERIC_9x1, id, $$2, container, 1);
    }
}
