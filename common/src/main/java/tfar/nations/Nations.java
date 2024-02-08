package tfar.nations;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tfar.nations.nation.Nation;
import tfar.nations.nation.NationData;
import tfar.nations.platform.Services;
import tfar.nations.sgui.api.ClickType;
import tfar.nations.sgui.api.elements.*;
import tfar.nations.sgui.api.gui.SimpleGui;

import java.util.List;
import java.util.UUID;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class Nations {

    public static final String MOD_ID = "nations";
    public static final String MOD_NAME = "Nations";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    // The loader specific projects are able to import and use any code from the common project. This allows you to
    // write the majority of your code here and load it from your loader specific projects. This example has some
    // code that gets invoked by the entry point of the loader specific projects.
    public static void init() {

        //   LOG.info("Hello from Common init on {}! we are currently in a {} environment!", Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
        //   LOG.info("The ID for diamonds is {}", BuiltInRegistries.ITEM.getKey(Items.DIAMOND));

        // It is common for all supported loaders to provide a similar feature that can not be used directly in the
        // common code. A popular way to get around this is using Java's built-in service loader feature to create
        // your own abstraction layer. You can learn more about this in our provided services class. In this example
        // we have an interface in the common code and use a loader specific implementation to delegate our call to
        // the platform specific approach.
        //    if (Services.PLATFORM.isModLoaded("examplemod")) {

        //        LOG.info("Hello to examplemod");
        //    }
    }

    public static void onCommandRegister(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(MOD_ID)
                .executes(Nations::openGui));

        dispatcher.register(Commands.literal("nationsop")
                .requires(p -> p.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(Nations::createNation)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.string()).suggests(NATIONS)
                                .executes(Nations::removeNation)))

        );
    }

    private static int createNation(CommandContext<CommandSourceStack> commandContext) {
        NationData nationData = getInstance(commandContext);
        String string = StringArgumentType.getString(commandContext, "name");
        if (nationData.createNation(string)) {
            return 1;
        }
        commandContext.getSource().sendFailure(Component.literal("Nation " + string + " already exists!"));
        return 0;
    }

    private static int removeNation(CommandContext<CommandSourceStack> commandContext) {
        NationData nationData = getInstance(commandContext);
        String string = StringArgumentType.getString(commandContext, "name");
        if (nationData.removeNation(string)) {
            commandContext.getSource().sendSuccess(Component.literal("Removed "+string+" Nation"),true);
            return 1;
        }
        commandContext.getSource().sendFailure(Component.literal("Nation " + string + " doesn't exist!"));
        return 0;
    }

    private static NationData getInstance(CommandContext<CommandSourceStack> commandContext) {
        MinecraftServer server = commandContext.getSource().getServer();
        return NationData.getDefaultNationsInstance(server);
    }

    private static final SuggestionProvider<CommandSourceStack> NATIONS = (commandContext, suggestionsBuilder) -> {
        List<String> collection = Nations.getInstance(commandContext).getNations().stream().map(Nation::getName).toList();
        return SharedSuggestionProvider.suggest(collection, suggestionsBuilder);
    };

    private static int openGui(CommandContext<CommandSourceStack> objectCommandContext) {
        try {
            ServerPlayer player = objectCommandContext.getSource().getPlayer();
            Nation nation = Services.PLATFORM.getNation(player);
            if (nation == null) {
                SimpleGui gui = new SimpleGui(MenuType.HOPPER, player, false);
                gui.setTitle(Component.literal("Create Nation?"));
                gui.setSlot(0, new GuiElementBuilder()
                        .setItem(Items.GREEN_WOOL)
                        .setName(Component.literal("Yes"))
                        .setCallback((index, clickType, actionType) -> {
                            ServerPlayer serverPlayer = gui.getPlayer();
                            NationData nationData = NationData.getDefaultNationsInstance(serverPlayer.server);
                            String name = serverPlayer.getGameProfile().getName();
                            nationData.createNation(name);
                            nationData.joinNation(name,List.of(serverPlayer));
                            serverPlayer.sendSystemMessage(Component.literal("Created Nation"));
                            gui.close();
                        })
                );
                gui.setSlot(4, new GuiElementBuilder()
                        .setItem(Items.RED_WOOL)
                        .setName(Component.literal("No"))
                        .setCallback((index, clickType, actionType) -> gui.close())
                );
                gui.open();
            } else {

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    private static int example(CommandContext<CommandSourceStack> objectCommandContext) {
        try {
            ServerPlayer player = objectCommandContext.getSource().getPlayer();
            SimpleGui gui = new SimpleGui(MenuType.GENERIC_3x3, player, false) {
                @Override
                public boolean onClick(int index, ClickType type, net.minecraft.world.inventory.ClickType action, GuiElementInterface element) {
                    this.player.sendSystemMessage(Component.literal(type.toString()), false);

                    return super.onClick(index, type, action, element);
                }

                @Override
                public void onTick() {
                    this.setSlot(0, new GuiElementBuilder(Items.ARROW).setCount((int) (player.level.getGameTime() % 127)));
                    super.onTick();
                }
            };

            gui.setTitle(Component.literal("Nice"));
            gui.setSlot(0, new GuiElementBuilder(Items.ARROW).setCount(100));
            gui.setSlot(1, new AnimatedGuiElement(new ItemStack[]{
                    Items.NETHERITE_PICKAXE.getDefaultInstance(),
                    Items.DIAMOND_PICKAXE.getDefaultInstance(),
                    Items.GOLDEN_PICKAXE.getDefaultInstance(),
                    Items.IRON_PICKAXE.getDefaultInstance(),
                    Items.STONE_PICKAXE.getDefaultInstance(),
                    Items.WOODEN_PICKAXE.getDefaultInstance()
            }, 10, false, (x, y, z) -> {
            }));

            gui.setSlot(2, new AnimatedGuiElementBuilder()
                    .setItem(Items.NETHERITE_AXE).setDamage(150).saveItemStack()
                    .setItem(Items.DIAMOND_AXE).setDamage(150).unbreakable().saveItemStack()
                    .setItem(Items.GOLDEN_AXE).glow().saveItemStack()
                    .setItem(Items.IRON_AXE).enchant(Enchantments.AQUA_AFFINITY, 1).hideFlags().saveItemStack()
                    .setItem(Items.STONE_AXE).saveItemStack()
                    .setItem(Items.WOODEN_AXE).saveItemStack()
                    .setInterval(10).setRandom(true)
            );

            for (int x = 3; x < gui.getSize(); x++) {
                ItemStack itemStack = Items.STONE.getDefaultInstance();
                itemStack.setCount(x);
                gui.setSlot(x, new GuiElement(itemStack, (index, clickType, actionType) -> {
                }));
            }

            gui.setSlot(5, new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setSkullOwner(
                            "ewogICJ0aW1lc3RhbXAiIDogMTYxOTk3MDIyMjQzOCwKICAicHJvZmlsZUlkIiA6ICI2OTBkMDM2OGM2NTE0OGM5ODZjMzEwN2FjMmRjNjFlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ5emZyXzciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDI0OGVhYTQxNGNjZjA1NmJhOTY5ZTdkODAxZmI2YTkyNzhkMGZlYWUxOGUyMTczNTZjYzhhOTQ2NTY0MzU1ZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                            null, null)
                    .setName(Component.literal("Battery"))
                    .glow()
            );

            gui.setSlot(6, new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setSkullOwner(new GameProfile(UUID.fromString("f5a216d9-d660-4996-8d0f-d49053677676"), "patbox"), player.server)
                    .setName(Component.literal("Patbox's Head"))
                    .glow()
            );

            gui.setSlot(7, new GuiElementBuilder()
                    .setItem(Items.BARRIER)
                    .glow()
                    .setName(Component.literal("Bye")
                            .setStyle(Style.EMPTY.withItalic(false).withBold(true)))
                    .addLoreLine(Component.literal("Some lore"))
                    .addLoreLine(Component.literal("More lore").withStyle(ChatFormatting.RED))
                    .setCount(3)
                    .setCallback((index, clickType, actionType) -> gui.close())
            );

            gui.setSlot(8, new GuiElementBuilder()
                    .setItem(Items.TNT)
                    .glow()
                    .setName(Component.literal("Test :)")
                            .setStyle(Style.EMPTY.withItalic(false).withBold(true)))
                    .addLoreLine(Component.literal("Some lore"))
                    .addLoreLine(Component.literal("More lore").withStyle(ChatFormatting.RED))
                    .setCount(1)
                    .setCallback((index, clickType, actionType) -> {
                        player.sendSystemMessage(Component.literal("derg "), false);
                        ItemStack item = gui.getSlot(index).getItemStack();
                        if (clickType == ClickType.MOUSE_LEFT) {
                            item.setCount(item.getCount() == 1 ? item.getCount() : item.getCount() - 1);
                        } else if (clickType == ClickType.MOUSE_RIGHT) {
                            item.setCount(item.getCount() + 1);
                        }
                        ((GuiElement) gui.getSlot(index)).setItemStack(item);

                        if (item.getCount() <= player.getEnderChestInventory().getContainerSize()) {
                            gui.setSlotRedirect(4, new Slot(player.getEnderChestInventory(), item.getCount() - 1, 0, 0));
                        }
                    })
            );
            gui.setSlotRedirect(4, new Slot(player.getEnderChestInventory(), 0, 0, 0));

            gui.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

}