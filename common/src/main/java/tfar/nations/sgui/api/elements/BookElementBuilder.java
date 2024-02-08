package tfar.nations.sgui.api.elements;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Book Element Builder
 * <br>
 * This is an GuiElementBuilder specifically designed for manipulating books.
 * Along with general manipulation from the GuiElementBuilder, it also
 * supplies multiple methods for manipulating pages, author, title, ect.
 *
 * @see GuiElementBuilderInterface
 */
@SuppressWarnings({"unused"})
public class BookElementBuilder extends GuiElementBuilder {

    /**
     * Constructs a new BookElementBuilder with the default settings.
     */
    public BookElementBuilder() {
        super(Items.WRITTEN_BOOK);
    }

    /**
     * Constructs a new BookElementBuilder with the supplied number
     * of items.
     *
     * @param count the number of items in the element
     */
    public BookElementBuilder(int count) {
        super(Items.WRITTEN_BOOK, count);
    }

    /**
     * Adds a new page to the book. <br>
     * Note that only signed books support formatting
     *
     * @param lines an array of lines, they will also wrap automatically to fit to the screen
     * @return this book builder
     * @see BookElementBuilder#setPage(int, Component...)
     */
    public BookElementBuilder addPage(Component... lines) {
        var text = Component.literal("");
        for (Component line : lines) {
            text.append(line).append("\n");
        }
        this.getOrCreatePages().add(StringTag.valueOf(Component.Serializer.toJson(text)));
        return this;
    }

    /**
     * Sets a page of the book. <br>
     * Note that only signed books support formatting
     *
     * @param index the page index, from 0
     * @param lines an array of lines, they will also wrap automatically to fit to the screen
     * @return this book builder
     * @throws IndexOutOfBoundsException if the page has not been created
     * @see BookElementBuilder#addPage(Component...)
     */
    public BookElementBuilder setPage(int index, Component... lines) {
        var text = Component.literal("");
        for (Component line : lines) {
            text.append(line).append("\n");
        }
        this.getOrCreatePages().set(index, StringTag.valueOf(Component.Serializer.toJson(text)));
        return this;
    }

    /**
     * Sets the author of the book, also marks
     * the book as signed.
     *
     * @param author the authors name
     * @return this book builder
     */
    public BookElementBuilder setAuthor(String author) {
        this.getOrCreateNbt().put("author", StringTag.valueOf(author));
        this.signed();
        return this;
    }

    /**
     * Sets the title of the book, also marks
     * the book as signed.
     *
     * @param title the book title
     * @return this book builder
     */
    public BookElementBuilder setTitle(String title) {
        this.getOrCreateNbt().put("title", StringTag.valueOf(title));
        this.signed();
        return this;
    }

    /**
     * Sets the book to be signed, not necessary
     * if already using setTitle or setAuthor.
     *
     * @return this book builder
     * @see BookElementBuilder#unSigned()
     */
    public BookElementBuilder signed() {
        this.setItem(Items.WRITTEN_BOOK);
        return this;
    }

    /**
     * Sets the book to not be signed, this will
     * also remove the title and author on
     * stack creation.
     *
     * @return this book builder
     * @see BookElementBuilder#signed() 
     */
    public BookElementBuilder unSigned() {
        this.setItem(Items.WRITABLE_BOOK);
        return this;
    }

    protected ListTag getOrCreatePages() {
        if (!this.getOrCreateNbt().contains("pages")) {
            this.getOrCreateNbt().put("pages", new ListTag());
        }
        return this.getOrCreateNbt().getList("pages", Tag.TAG_STRING);
    }

    @Override
    public GuiElementBuilder setItem(Item item) {
        if (!(item.builtInRegistryHolder().is(ItemTags.LECTERN_BOOKS))) {
            throw new IllegalArgumentException("Item must be a type of book");
        }

        return super.setItem(item);
    }

    /**
     * Only written books may have formatting, thus if the book is not marked as signed,
     * we must strip the formatting. To sign a book use the {@link BookElementBuilder#setTitle(String)}
     * or {@link BookElementBuilder#setAuthor(String)} methods.
     *
     * @return the book as a stack
     */
    @Override
    public ItemStack asStack() {
        if (this.item == Items.WRITTEN_BOOK) {
            if (!this.getOrCreateNbt().contains("author")) {
                this.getOrCreateNbt().put("author", StringTag.valueOf(""));
            }
            if (!this.getOrCreateNbt().contains("title")) {
                this.getOrCreateNbt().put("title", StringTag.valueOf(""));
            }
        } else if (this.item == Items.WRITABLE_BOOK){
            ListTag pages = this.getOrCreatePages();
            for (int i = 0; i < pages.size(); i++) {
                try {
                    pages.set(i, StringTag.valueOf(Component.Serializer.fromJsonLenient(pages.getString(i)).getString()));
                } catch (Exception e) {
                    pages.set(i, StringTag.valueOf("Invalid page data!"));
                }
            }
            this.getOrCreateNbt().put("pages", pages);

            this.getOrCreateNbt().remove("author");
            this.getOrCreateNbt().remove("title");
        }

        return super.asStack();
    }

    /**
     * Constructs BookElementBuilder based on the supplied book.
     * Useful for making changes to existing books.
     * <br>
     * The method will check for the existence of a 'title'
     * and 'author' tag, if either is found it will assume
     * the book has been signed. This can be undone
     * with the {@link BookElementBuilder#unSigned()}.
     *
     *
     * @param book the target book stack
     * @return the builder
     * @throws IllegalArgumentException if the stack is not a book
     */
    public static BookElementBuilder from(ItemStack book) {
        if (!book.getItem().builtInRegistryHolder().is(ItemTags.LECTERN_BOOKS)) {
            throw new IllegalArgumentException("Item must be a type of book");
        }

        BookElementBuilder builder = new BookElementBuilder(book.getCount());

        if (book.getOrCreateTag().contains("title")) {
            builder.setTitle(book.getOrCreateTag().getString("title"));
        }

        if (book.getOrCreateTag().contains("author")) {
            builder.setTitle(book.getOrCreateTag().getString("author"));
        }

        if (book.getOrCreateTag().contains("pages")) {
            ListTag pages = book.getOrCreateTag().getList("pages", Tag.TAG_STRING);
            for (Tag page : pages) {
                builder.addPage(Component.Serializer.fromJsonLenient(page.getAsString()));
            }
        }

        return builder;
    }

    /**
     * Returns the contents of the specified page.
     *
     * @param book  the book to get the page from
     * @param index the page index, from 0
     * @return the contents of the page or empty if page does not exist
     * @throws IllegalArgumentException if the item is not a book
     */
    public static Component getPageContents(ItemStack book, int index) {
        if (!book.getItem().builtInRegistryHolder().is(ItemTags.LECTERN_BOOKS)) {
            throw new IllegalArgumentException("Item must be a type of book");
        }

        if (book.getOrCreateTag().contains("pages")) {
            ListTag pages = book.getOrCreateTag().getList("pages", Tag.TAG_STRING);
            if(index < pages.size()) {
                return Component.Serializer.fromJson(pages.get(index).getAsString());
            }
        }

        return Component.literal("");
    }

    /**
     * Returns the contents of the specified page.
     *
     * @param book  the book element builder to get the page from
     * @param index the page index, from 0
     * @return the contents of the page or empty if page does not exist
     */
    public static Component getPageContents(BookElementBuilder book, int index) {
        ListTag pages = book.getOrCreatePages();
        if(index < pages.size()) {
            return Component.Serializer.fromJson(pages.get(index).getAsString());
        }
        return Component.literal("");
    }

}
