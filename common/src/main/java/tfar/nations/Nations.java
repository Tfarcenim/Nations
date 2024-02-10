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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        if (nationData.createNation(string) != null) {
            return 1;
        }
        commandContext.getSource().sendFailure(Component.literal("Nation " + string + " already exists!"));
        return 0;
    }

    private static int removeNation(CommandContext<CommandSourceStack> commandContext) {
        NationData nationData = getInstance(commandContext);
        String string = StringArgumentType.getString(commandContext, "name");
        if (nationData.removeNation(string)) {
            commandContext.getSource().sendSuccess(Component.literal("Removed " + string + " Nation"), true);
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
            ServerPlayer player = objectCommandContext.getSource().getPlayerOrException();
            Nation existingNation = Services.PLATFORM.getNation(player);
            NationData nationData = NationData.getDefaultNationsInstance(player.server);

            Nation invitedTo = nationData.getInviteForPlayer(player);

            if (invitedTo != null) {
                SimpleGui inviteGui = new SimpleGui(MenuType.HOPPER, player, false);
                inviteGui.setTitle(Component.literal("Accept invite to " + invitedTo.getName() + " ?"));
                inviteGui.setSlot(0, new GuiElementBuilder()
                        .setItem(Items.GREEN_WOOL)
                        .setName(Component.literal("Yes"))
                        .setCallback((index, clickType, actionType) -> {
                            nationData.joinNation(invitedTo.getName(), List.of(player));
                            nationData.removeInvite(player);
                            player.sendSystemMessage(Component.literal("You are now part of " + invitedTo.getName() + " nation"), false);
                            inviteGui.close();
                        })
                );
                inviteGui.setSlot(4, new GuiElementBuilder()
                        .setItem(Items.RED_WOOL)
                        .setName(Component.literal("No"))
                        .setCallback((index, clickType, actionType) -> {
                            nationData.removeInvite(player);
                            inviteGui.close();
                        })
                );
                inviteGui.open();
                return 1;
            }

            if (existingNation == null) {
                SimpleGui gui = new SimpleGui(MenuType.HOPPER, player, false);
                gui.setTitle(Component.literal("Create Nation?"));
                gui.setSlot(0, new GuiElementBuilder()
                        .setItem(Items.GREEN_WOOL)
                        .setName(Component.literal("Yes"))
                        .setCallback((index, clickType, actionType) -> {
                            ServerPlayer serverPlayer = gui.getPlayer();
                            String name = serverPlayer.getGameProfile().getName();
                            nationData.createNation(name);
                            nationData.setOwner(name, serverPlayer);
                            serverPlayer.sendSystemMessage(Component.literal("Created Nation " + name));
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
                if (existingNation.isOwner(player)) {
                    openTeamLeaderGui(nationData, existingNation, player);
                } else {
                    openTeamMemberGui(nationData,existingNation,player);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    private static void openTeamMemberGui(NationData nationData, Nation existingNation, ServerPlayer player) {
        SimpleGui teamMemberMenu = new SimpleGui(MenuType.HOPPER,player,false);
        teamMemberMenu.setTitle(Component.literal("Nation Member Menu"));
        teamMemberMenu.setSlot(4,new GuiElementBuilder()
                .setItem(Items.BARRIER)
                .setName(Component.literal("Leave Nation"))
                .setCallback((index, type, action) -> {
                    SimpleGui confirmGui = new SimpleGui(MenuType.HOPPER, player, false);
                    confirmGui.setTitle(Component.literal("Leave Nation?"));
                    confirmGui.setSlot(0, new GuiElementBuilder()
                            .setItem(Items.GREEN_WOOL)
                            .setName(Component.literal("Yes"))
                            .setCallback((index1, clickType1, actionType1) -> {
                                ServerPlayer serverPlayer = confirmGui.getPlayer();
                                nationData.leaveNation(List.of(serverPlayer));
                                serverPlayer.sendSystemMessage(Component.literal("Left Nation " + existingNation.getName()));
                                confirmGui.close();
                            })
                    );
                    confirmGui.setSlot(4, new GuiElementBuilder()
                            .setItem(Items.RED_WOOL)
                            .setName(Component.literal("No"))
                            .setCallback((index1, clickType1, actionType1) -> confirmGui.close())
                    );
                    confirmGui.open();
                })
        );
        teamMemberMenu.open();
    }

    private static void openTeamLeaderGui(NationData nationData, Nation existingNation, ServerPlayer player) {
        SimpleGui teamLeaderMenu = new SimpleGui(MenuType.HOPPER, player, false);
        teamLeaderMenu.setTitle(Component.literal("Nation Leader Menu"));
        teamLeaderMenu.setSlot(0, new GuiElementBuilder()
                .setItem(Items.BARRIER)
                .setName(Component.literal("Disband Nation"))
                .setCallback((index, clickType, actionType) -> {
                    SimpleGui confirmGui = new SimpleGui(MenuType.HOPPER, player, false);
                    confirmGui.setTitle(Component.literal("Disband Nation?"));
                    confirmGui.setSlot(0, new GuiElementBuilder()
                            .setItem(Items.GREEN_WOOL)
                            .setName(Component.literal("Yes"))
                            .setCallback((index1, clickType1, actionType1) -> {
                                ServerPlayer serverPlayer = confirmGui.getPlayer();
                                nationData.removeNation(existingNation.getName());
                                serverPlayer.sendSystemMessage(Component.literal("Disbanded Nation " + existingNation.getName()));
                                confirmGui.close();
                            })
                    );
                    confirmGui.setSlot(4, new GuiElementBuilder()
                            .setItem(Items.RED_WOOL)
                            .setName(Component.literal("No"))
                            .setCallback((index1, clickType1, actionType1) -> confirmGui.close())
                    );
                    confirmGui.open();
                }));

        teamLeaderMenu.setSlot(1, new GuiElementBuilder()
                .setItem(Items.LEAD)
                .setName(Component.literal("Manage Players"))
                .setCallback((index, type, action) -> {
                    SimpleGui managePlayers = new SimpleGui(MenuType.HOPPER, player, false);
                    managePlayers.setTitle(Component.literal("Manage Players"));

                    managePlayers.setSlot(0, new GuiElementBuilder()
                            .setItem(Items.PAPER)
                            .setName(Component.literal("Invite Players"))
                            .setCallback((index1, clickType1, actionType1) -> {
                                SimpleGui inviteGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);

                                inviteGui.setTitle(Component.literal("Invite Players"));
                                List<ServerPlayer> eligible = getUninvitedPlayers(player, existingNation);
                                int i = 0;
                                for (ServerPlayer invitePlayer : eligible) {
                                    GuiElementBuilder elementBuilder = new GuiElementBuilder();
                                    inviteGui.setSlot(i, elementBuilder
                                            .setItem(Items.PLAYER_HEAD)
                                            .setSkullOwner(invitePlayer.getGameProfile(), player.server)
                                            .setName(invitePlayer.getName())
                                            .setCallback(
                                                    (index2, type1, action1, gui) -> {
                                                        nationData.sendInvites(List.of(invitePlayer.getGameProfile()), existingNation);
                                                        gui.close();
                                                    }));
                                }

                           /*     inviteGui.setSlot(26, new GuiElementBuilder()
                                        .setItem(Items.FEATHER)
                                        .setName(Component.literal("Send Invites"))
                                        .setCallback((index2, type1, action1, gui) -> {
                                            List<GameProfile> actuallyInvite = new ArrayList<>();
                                            for (int j = 0; j < eligible.size();j++) {
                                                GuiElementInterface slot = gui.getSlot(j);
                                                if (slot != null) {
                                                    if (slot.getItemStack().hasFoil()) {
                                                        GameProfile gameProfile = NbtUtils.readGameProfile(slot.getItemStack()
                                                                .getTag().getCompound(SkullBlockEntity.TAG_SKULL_OWNER));
                                                        actuallyInvite.add(gameProfile);
                                                    }
                                                }
                                            }
                                            nationData.sendInvites(actuallyInvite,existingNation);
                                        })
                                );*/

                                inviteGui.open();
                            }));

                    managePlayers.setSlot(1, new GuiElementBuilder()
                            .setItem(Items.IRON_SWORD)
                            .setName(Component.literal("Exile Players"))
                            .setCallback((index1, clickType1, actionType1) -> {
                                SimpleGui exileGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
                                exileGui.setTitle(Component.literal("Exile Players"));
                                List<GameProfile> members = getAllTeamMembers(player,existingNation);
                                int i = 0;
                                for (GameProfile gameProfile : members) {
                                    GuiElementBuilder elementBuilder = new GuiElementBuilder();
                                    String name = gameProfile.getName();
                                    if (player.server.getPlayerList().getPlayer(gameProfile.getId()) == null) {
                                        name+=" (Offline)";
                                    }
                                    exileGui.setSlot(i, elementBuilder
                                            .setItem(Items.PLAYER_HEAD)
                                            .setSkullOwner(gameProfile, player.server)
                                            .setName(Component.literal(name))
                                            .setCallback(
                                                    (index2, type1, action1, gui) -> {
                                                        nationData.leaveNationGameProfiles(player.server, List.of(gameProfile));
                                                        player.sendSystemMessage(Component.literal(gameProfile.getName()+" has been exiled"));
                                                        gui.close();
                                                    }));
                                }

                                exileGui.open();

                            }));

                    managePlayers.open();
                })
        );

        teamLeaderMenu.open();
    }

    private static List<ServerPlayer> getUninvitedPlayers(ServerPlayer leader, Nation nation) {
        List<ServerPlayer> allPlayers = new ArrayList<>(leader.server.getPlayerList().getPlayers());
        allPlayers.removeIf(player -> Services.PLATFORM.getNation(player) != null);
        return allPlayers;
    }

    private static List<GameProfile> getAllTeamMembers(ServerPlayer leader, Nation nation) {
        Set<GameProfile> members = nation.getMembers();
        List<GameProfile> list = new ArrayList<>(members);
        list.remove(leader.getGameProfile());
        return list;
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