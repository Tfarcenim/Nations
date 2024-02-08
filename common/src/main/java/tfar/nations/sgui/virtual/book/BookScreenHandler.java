package tfar.nations.sgui.virtual.book;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tfar.nations.sgui.api.gui.BookGui;
import tfar.nations.sgui.virtual.VirtualScreenHandlerInterface;
import tfar.nations.sgui.virtual.inventory.VirtualSlot;

public class BookScreenHandler extends AbstractContainerMenu implements VirtualScreenHandlerInterface {
    private final BookGui gui;

    public BookScreenHandler(int syncId, BookGui gui, Player player) {
        super(MenuType.LECTERN, syncId);
        this.gui = gui;

        this.addSlot(new BookSlot(new BookInventory(gui), 0, 0, 0));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        switch (id) {
            case 1 -> {
                this.gui.setPage(gui.getPage() - 1);
                return true;
            }
            case 2 -> {
                this.gui.setPage(gui.getPage() + 1);
                return true;
            }
            case 3 -> {
                this.gui.onTakeBookButton();
                return true;
            }
        }
        if (id >= 100) {
            this.gui.setPage(id - 100);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void setItem(int slot, int i, ItemStack stack) {
        if (slot == 0) {
            this.getSlot(slot).set(stack);
        } else {
            this.getSlot(slot).set(ItemStack.EMPTY);
        }
    }

    @Override
    public void broadcastChanges() {
        try {
            this.gui.onTick();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !(slot instanceof VirtualSlot) && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean fromLast) {
        return false;
    }

    @Override
    public BookGui getGui() {
        return gui;
    }
}
