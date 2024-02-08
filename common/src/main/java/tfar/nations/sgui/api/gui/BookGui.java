package tfar.nations.sgui.api.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import tfar.nations.sgui.api.ScreenProperty;
import tfar.nations.sgui.api.elements.BookElementBuilder;
import tfar.nations.sgui.virtual.SguiScreenHandlerFactory;
import tfar.nations.sgui.virtual.book.BookScreenHandler;

import java.util.OptionalInt;

/**
 * Book Gui Implementation
 * <p>
 * BookGui is used to display book pages to the player. A pre-existing book needs
 * to be passed into the constructor, this is what will be displayed.
 * <p>
 * BookGui has lots of deprecated methods which have no function, this is
 * mainly due to the lack of item slots in the book interface.
 */
@SuppressWarnings("unused")
public class BookGui implements GuiInterface {
    protected final ServerPlayer player;
    protected ItemStack book;
    protected int page = 0;
    protected boolean open = false;
    protected boolean reOpen = false;
    protected BookScreenHandler screenHandler = null;

    protected int syncId = -1;

    /**
     * Constructs a new BookGui for the supplied player, based
     * on the provided book.
     *
     * @param player the player to serve this gui to
     * @param book   the book stack to display
     * @throws IllegalArgumentException if the provided item is not a book
     */
    public BookGui(ServerPlayer player, ItemStack book) {
        this.player = player;

        if (!book.getItem().builtInRegistryHolder().is(ItemTags.LECTERN_BOOKS)) {
            throw new IllegalArgumentException("Item must be a book");
        }
        this.book = book;
    }

    /**
     * Constructs a new BookGui for the supplied player, based
     * on the provided book.
     *
     * @param player the player to serve this gui to
     * @param book   the book builder to display
     */
    public BookGui(ServerPlayer player, BookElementBuilder book) {
        this.player = player;
        this.book = book.asStack();
    }

    /**
     * Sets the selected page number
     *
     * @param page the page index, from 0
     */
    public void setPage(int page) {
        this.page = page;
        this.sendProperty(ScreenProperty.SELECTED, this.page);
    }

    /**
     * Returns the current selected page
     *
     * @return the page index, from 0
     */
    public int getPage() {
        return page;
    }

    /**
     * Returns the book item used to store the data.
     *
     * @return the book stack
     */
    public ItemStack getBook() {
        return this.book;
    }

    /**
     * Activates when the 'Take Book' button is pressed
     */
    public void onTakeBookButton() {
    }

    @Override
    public MenuType<?> getType() { return MenuType.LECTERN; }

    @Override
    public ServerPlayer getPlayer() {
        return this.player;
    }

    @Override
    public int getSyncId() {
        return this.syncId;
    }

    @Override
    public boolean isOpen() {
        return this.open;
    }

    @Override
    public boolean open() {
        if (!this.player.hasDisconnected() && !this.open) {
            this.open = true;
            this.onOpen();
            this.reOpen = true;
            OptionalInt temp = this.player.openMenu(new SguiScreenHandlerFactory<>(this, (syncId, inv, player) -> new BookScreenHandler(syncId, this, player)));
            this.reOpen = false;
            if (temp.isPresent()) {
                this.syncId = temp.getAsInt();
                if (this.player.containerMenu instanceof BookScreenHandler) {
                    this.screenHandler = (BookScreenHandler) this.player.containerMenu;
                    this.sendProperty(ScreenProperty.SELECTED, this.page);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Called when player executes command via {@link net.minecraft.network.chat.ClickEvent.Action#RUN_COMMAND}
     *
     * @param command input command
     * @return Returns false, for continuing execution or true, if you want to cancel it
     */
    public boolean onCommand(String command) {
        return false;
    }

    @Override
    public void close(boolean screenHandlerIsClosed) {
        if (this.open && !this.reOpen) {
            this.open = false;
            this.reOpen = false;

            if (!screenHandlerIsClosed && this.player.containerMenu == this.screenHandler) {
                this.player.closeContainer();
            }

            this.onClose();
        } else {
            this.reOpen = false;
        }
    }

    @Deprecated
    @Override
    public void setTitle(Component title) {
    }

    @Deprecated
    @Override
    public Component getTitle() {
        return null;
    }

    @Deprecated
    @Override
    public boolean getAutoUpdate() {
        return false;
    }

    @Deprecated
    @Override
    public void setAutoUpdate(boolean value) {
    }
}
