package tfar.nations.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NoSlot extends Slot {
    public NoSlot(Container $$0, int $$1, int $$2, int $$3) {
        super($$0, $$1, $$2, $$3);
    }

    @Override
    public void onTake(Player $$0, ItemStack $$1) {
        super.onTake($$0, $$1);
    }
}
