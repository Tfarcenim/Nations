package tfar.nations.sgui.virtual.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class VirtualSlot extends Slot {

    public VirtualSlot(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mayPickup(Player playerEntity) {
        return false;
    }

    @Override
    public boolean allowModification(Player player) {
        return false;
    }

    @Override
    public ItemStack safeInsert(ItemStack stack, int count) {
        return stack;
    }


    @Override
    public void set(ItemStack stack) {

    }

    @Override
    public boolean hasItem() {
        return true;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}
