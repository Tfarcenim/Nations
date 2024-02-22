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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tfar.nations.level.OfflineTrackerData;
import tfar.nations.nation.Nation;
import tfar.nations.nation.NationData;
import tfar.nations.platform.Services;
import tfar.nations.sgui.api.ClickType;
import tfar.nations.sgui.api.elements.*;
import tfar.nations.sgui.api.gui.SimpleGui;

import java.util.*;
import java.util.function.UnaryOperator;

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

    public static void login(ServerPlayer player) {

        NationData nationData = NationData.getOrCreateDefaultNationsInstance(player.server);
        Siege siege = nationData.getActiveSiege();
        if (siege != null && siege.isPlayerInvolved(player)) {
            nationData.endSiege(Siege.Result.TERMINATED);
        }

        TeamHandler.updateSelf(player);
        TeamHandler.updateOthers(player);
    }

    public static void logout(ServerPlayer player) {
        NationData nationData = NationData.getOrCreateDefaultNationsInstance(player.server);
        Siege siege = nationData.getActiveSiege();
        if (siege != null) {
            siege.attackerDefeated(player);
            siege.defenderDefeated(player);
        }

        OfflineTrackerData offlineTrackerData = OfflineTrackerData.getOrCreateDefaultInstance(player.server);
        offlineTrackerData.saveTimeStamp(player);

//        TeamHandler.updateSelf(player);
  //      TeamHandler.updateOthers(player);
    }


    public static final Item YES = Items.GREEN_STAINED_GLASS_PANE;
    public static final Item NO = Items.RED_STAINED_GLASS_PANE;
    public static final Item BLANK = Items.LIGHT_GRAY_STAINED_GLASS_PANE;

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
                .then(Commands.literal("siege")
                        .then(Commands.literal("stop")
                                .executes(Nations::stopSiege)
                        ))

        );
    }

    private static int stopSiege(CommandContext<CommandSourceStack> commandContext) {
        NationData nationData = getOverworldInstance(commandContext);
        boolean worked = nationData.endSiege(Siege.Result.TERMINATED);
        if (worked) {
            commandContext.getSource().sendSuccess(Component.literal("Active siege cancelled"),false);
            return 1;
        } else {
            commandContext.getSource().sendFailure(Component.literal("No active siege"));
            return 0;
        }
    }

    private static int createNation(CommandContext<CommandSourceStack> commandContext) {
        NationData nationData = getOverworldInstance(commandContext);
        String string = StringArgumentType.getString(commandContext, "name");
        if (nationData.createNation(string) != null) {
            return 1;
        }
        commandContext.getSource().sendFailure(Component.literal("Nation " + string + " already exists!"));
        return 0;
    }

    private static int removeNation(CommandContext<CommandSourceStack> commandContext) {
        NationData nationData = getOverworldInstance(commandContext);
        String string = StringArgumentType.getString(commandContext, "name");
        if (nationData.removeNation(commandContext.getSource().getServer(), string)) {
            commandContext.getSource().sendSuccess(Component.literal("Removed " + string + " Nation"), true);
            return 1;
        }
        commandContext.getSource().sendFailure(Component.literal("Nation " + string + " doesn't exist!"));
        return 0;
    }

    private static NationData getOverworldInstance(CommandContext<CommandSourceStack> commandContext) {
        MinecraftServer server = commandContext.getSource().getServer();
        return NationData.getOrCreateDefaultNationsInstance(server);
    }

    private static final SuggestionProvider<CommandSourceStack> NATIONS = (commandContext, suggestionsBuilder) -> {
        List<String> collection = Nations.getOverworldInstance(commandContext).getNations().stream().map(Nation::getName).toList();
        return SharedSuggestionProvider.suggest(collection, suggestionsBuilder);
    };

    private static int openGui(CommandContext<CommandSourceStack> objectCommandContext) {
        try {
            ServerPlayer player = objectCommandContext.getSource().getPlayerOrException();

            if (player.getLevel().dimension() != Level.OVERWORLD) {
                player.sendSystemMessage(Component.literal("Can't use Nations outside of overworld"));
                return 0;
            }

            Nation existingNation = Services.PLATFORM.getNation(player);
            NationData nationData = NationData.getOrCreateDefaultNationsInstance(player.server);

            Nation invitedTo = nationData.getInviteForPlayer(player);

            if (invitedTo != null) {
                SimpleGui inviteGui = new SimpleGui(MenuType.HOPPER, player, false);
                inviteGui.setTitle(Component.literal("Accept invite to " + invitedTo.getName() + " ?"));
                inviteGui.setSlot(0, new GuiElementBuilder()
                        .setItem(YES)
                        .setName(Component.literal("Yes"))
                        .setCallback((index, clickType, actionType) -> {
                            nationData.joinNation(invitedTo.getName(), List.of(player));
                            nationData.removeInvite(player);
                            player.sendSystemMessage(Component.literal("You are now part of " + invitedTo.getName() + " nation"), false);
                            inviteGui.close();
                        })
                );
                inviteGui.setSlot(4, new GuiElementBuilder()
                        .setItem(NO)
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
                        .setItem(YES)
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
                        .setItem(NO)
                        .setName(Component.literal("No"))
                        .setCallback((index, clickType, actionType) -> gui.close())
                );
                gui.open();
            } else {
                if (existingNation.isOwner(player)) {
                    openTeamLeaderGui(nationData, existingNation, player);
                } else {
                    if (existingNation.isOfficer(player)) {
                        openTeamOfficerMenu(nationData, existingNation, player);
                    } else {
                        openTeamMemberMenu(nationData, existingNation, player);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    private static void openTeamOfficerMenu(NationData nationData, Nation existingNation, ServerPlayer player) {
        SimpleGui teamOfficerMenu = new SimpleGui(MenuType.HOPPER, player, false);
        teamOfficerMenu.setTitle(Component.literal("Nation Officer Menu"));
        teamOfficerMenu.setSlot(0, ServerButtons.managePlayersButton(player, nationData, existingNation));
        teamOfficerMenu.setSlot(1, ServerButtons.topNationsButton(player, nationData));
        teamOfficerMenu.setSlot(2, ServerButtons.onlinePlayersButton(player, nationData));
        teamOfficerMenu.setSlot(4, ServerButtons.leaveTeamButton(player, nationData, existingNation));
        teamOfficerMenu.open();
    }

    private static void openTeamMemberMenu(NationData nationData, Nation existingNation, ServerPlayer player) {
        SimpleGui teamMemberMenu = new SimpleGui(MenuType.HOPPER, player, false);
        teamMemberMenu.setTitle(Component.literal("Nation Member Menu"));
        teamMemberMenu.setSlot(0, ServerButtons.topNationsButton(player, nationData));
        teamMemberMenu.setSlot(1, ServerButtons.onlinePlayersButton(player, nationData));
        teamMemberMenu.setSlot(2, ServerButtons.leaveTeamButton(player, nationData, existingNation));
        teamMemberMenu.open();
    }

    private static void openTeamLeaderGui(NationData nationData, Nation existingNation, ServerPlayer player) {
        SimpleGui teamLeaderMenu = new SimpleGui(MenuType.GENERIC_9x1, player, false);
        teamLeaderMenu.setTitle(Component.literal("Nation Leader Menu"));
        teamLeaderMenu.setSlot(0, new GuiElementBuilder()
                .setItem(Items.BARRIER)
                .setName(Component.literal("Disband Nation"))
                .setCallback((index, clickType, actionType) -> {
                    SimpleGui confirmGui = new SimpleGui(MenuType.HOPPER, player, false);
                    confirmGui.setTitle(Component.literal("Disband Nation?"));
                    confirmGui.setSlot(0, new GuiElementBuilder()
                            .setItem(YES)
                            .setName(Component.literal("Yes"))
                            .setCallback((index1, clickType1, actionType1) -> {
                                ServerPlayer serverPlayer = confirmGui.getPlayer();
                                nationData.removeNation(player.server, existingNation.getName());
                                serverPlayer.sendSystemMessage(Component.literal("Disbanded Nation " + existingNation.getName()));
                                confirmGui.close();
                            })
                    );
                    confirmGui.setSlot(4, new GuiElementBuilder()
                            .setItem(NO)
                            .setName(Component.literal("No"))
                            .setCallback((index1, clickType1, actionType1) -> confirmGui.close())
                    );
                    confirmGui.open();
                }));

        teamLeaderMenu.setSlot(1, ServerButtons.managePlayersButton(player, nationData, existingNation));
        teamLeaderMenu.setSlot(2,ServerButtons.claimChunksButton2(player, nationData, existingNation));
        teamLeaderMenu.setSlot(3,ServerButtons.unClaimChunksButton1(player, nationData, existingNation));

        teamLeaderMenu.setSlot(4, new GuiElementBuilder()
                .setItem(Items.SHIELD)
                .setName(Component.literal("Nation Politics"))
                .setCallback((index, type, action, gui) -> {

                    Nation allianceInvite = nationData.getAllianceInvite(existingNation);

                    if (allianceInvite != null) {
                        SimpleGui inviteGui = new SimpleGui(MenuType.HOPPER, player, false);
                        inviteGui.setTitle(Component.literal("Accept alliance invite to " + allianceInvite.getName() + " ?"));
                        inviteGui.setSlot(0, new GuiElementBuilder()
                                .setItem(YES)
                                .setName(Component.literal("Yes"))
                                .setCallback((index1, clickType, actionType) -> {
                                    nationData.createAllianceBetween(player.server, allianceInvite, existingNation);
                                    nationData.removeAllyInvite(allianceInvite, existingNation);
                                    player.sendSystemMessage(Component.literal("You are now allied with " + allianceInvite.getName() + " nation"), false);
                                    inviteGui.close();
                                })
                        );
                        inviteGui.setSlot(4, new GuiElementBuilder()
                                .setItem(NO)
                                .setName(Component.literal("No"))
                                .setCallback((index1, clickType, actionType) -> {
                                    nationData.removeAllyInvite(allianceInvite, existingNation);
                                    inviteGui.close();
                                })
                        );
                        inviteGui.open();
                    } else {

                        SimpleGui politicsGui = new SimpleGui(MenuType.HOPPER, player, false);
                        politicsGui.setTitle(Component.literal("Nation Politics"));
                        politicsGui.setSlot(0, new GuiElementBuilder()
                                .setItem(Items.FEATHER)
                                .setName(Component.literal("Make Alliance"))
                                .setCallback((index1, type1, action1, gui1) -> {
                                    SimpleGui allianceGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
                                    allianceGui.setTitle(Component.literal("Make Alliance"));
                                    List<Nation> nonAlliedLeaders = nationData.getNonAllianceNations(existingNation);
                                    for (int i = 0; i < nonAlliedLeaders.size(); i++) {
                                        Nation nation = nonAlliedLeaders.get(i);
                                        allianceGui.setSlot(i, new GuiElementBuilder(Items.PLAYER_HEAD)
                                                .setSkullOwner(nation.getOwner(), player.server)
                                                .setName(Component.literal(nation.getOwner().getName()))

                                                .setCallback((index2, type2, action2, gui2) -> {
                                                    nationData.sendAllyInvites(existingNation, nation);
                                                    gui2.close();
                                                })
                                        );
                                    }
                                    allianceGui.open();
                                })

                        );

                        politicsGui.setSlot(1, new GuiElementBuilder()
                                .setItem(Items.NETHERITE_SWORD)
                                .hideFlags()
                                .setName(Component.literal("Make Enemy"))
                                .setCallback((index1, type1, action1, gui1) -> {
                                    SimpleGui enemyGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
                                    enemyGui.setTitle(Component.literal("Make Enemy"));
                                    List<Nation> nonAlliedLeaders = nationData.getNonEnemyNations(existingNation);
                                    for (int i = 0; i < nonAlliedLeaders.size(); i++) {
                                        Nation nation = nonAlliedLeaders.get(i);
                                        enemyGui.setSlot(i, new GuiElementBuilder(Items.PLAYER_HEAD)
                                                .setSkullOwner(nation.getOwner(), player.server)
                                                .setName(Component.literal(nation.getOwner().getName()))
                                                .setCallback((index2, type2, action2, gui2) -> {
                                                    nationData.makeEnemy(player.server, existingNation, nation);
                                                    gui2.close();
                                                })
                                        );
                                    }
                                    enemyGui.open();
                                })

                        );

                        politicsGui.setSlot(2, new GuiElementBuilder()
                                .setItem(Items.ENDER_EYE)
                                .setName(Component.literal("Make Neutral"))
                                .setCallback((index1, type1, action1, gui1) -> {

                                    SimpleGui enemyGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
                                    enemyGui.setTitle(Component.literal("Make Neutral"));
                                    List<Nation> nonAlliedLeaders = nationData.getNonNeutralNations(existingNation);
                                    for (int i = 0; i < nonAlliedLeaders.size(); i++) {
                                        Nation nation = nonAlliedLeaders.get(i);
                                        boolean isFriendly = nation.isAlly(existingNation);
                                        enemyGui.setSlot(i, new GuiElementBuilder(Items.PLAYER_HEAD)
                                                .setSkullOwner(nation.getOwner(), player.server)
                                                .setName(Component.literal(nation.getOwner().getName() + " - " + (isFriendly ? "Allied" : "Enemy")))
                                                .setCallback((index2, type2, action2, gui2) -> {
                                                    nationData.makeNeutral(player.server, existingNation, nation);
                                                    gui2.close();
                                                })
                                        );
                                    }
                                    enemyGui.open();

                                })

                        );

                        politicsGui.setSlot(3, new GuiElementBuilder()
                                .setItem(Items.BOOK)
                                .setName(Component.literal("Nation Status"))
                                .setCallback((index1, type1, action1, gui1) -> {
                                    SimpleGui statusGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
                                    statusGui.setTitle(Component.literal("Nation Status"));
                                    List<Nation> nonAlliedLeaders = nationData.getNonNeutralNations(existingNation);
                                    for (int i = 0; i < nonAlliedLeaders.size(); i++) {
                                        Nation nation = nonAlliedLeaders.get(i);
                                        boolean isFriendly = nation.isAlly(existingNation);
                                        statusGui.setSlot(i, new GuiElementBuilder(Items.PLAYER_HEAD)
                                                .setSkullOwner(nation.getOwner(), player.server)
                                                .setName(Component.literal(nation.getOwner().getName() + " - " + (isFriendly ? "Allied" : "Enemy")))
                                                .setCallback((index2, type2, action2, gui2) -> {
                                                })
                                        );
                                    }
                                    statusGui.open();
                                })
                        );
                        politicsGui.open();
                    }
                })
        );

        teamLeaderMenu.setSlot(5, ServerButtons.topNationsButton(player, nationData));
        teamLeaderMenu.setSlot(6, ServerButtons.onlinePlayersButton(player, nationData));
        teamLeaderMenu.setSlot(7, new GuiElementBuilder(Items.RED_BANNER)
                .setName(Component.literal("Declare Siege"))
                .setCallback((index, type, action, gui) -> {
                    SimpleGui raidGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
                    raidGui.setTitle(Component.literal("Select Enemy Nation"));
                    Set<String> enemies = existingNation.getEnemies();
                    int i = 0;
                    for (String s : enemies) {
                        Nation enemyNation = nationData.getNationByName(s);

                        if (enemyNation != null) {

                            List<ServerPlayer> online = enemyNation.getOnlineMembers(player.server);
                            int total = enemyNation.getMembers().size();

                            double percentage = online.size() / (double) total;

                            String name = enemyNation.getName() + " | " + online.size() + "/" + total + " online";

                            raidGui.setSlot(i++, new GuiElementBuilder(Items.PLAYER_HEAD)
                                    .setSkullOwner(enemyNation.getOwner(), player.server)
                                    .setName(Component.literal(name))
                                    .setCallback((index1, type1, action1, gui1) -> {

                                        if (percentage >= .5) {
                                            SimpleGui siege2Gui = new SimpleGui(MenuType.GENERIC_9x6, player, false);
                                            siege2Gui.setTitle(Component.literal("Select Enemy Claim"));
                                            ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                                            int i1 = 0;
                                            for (int z = -2; z < 4; z++) {
                                                for (int x = -4; x < 5; x++) {
                                                    ChunkPos offset = new ChunkPos(chunkPos.x + x, chunkPos.z + z);

                                                    Nation claimed = nationData.getNationAtChunk(offset);
                                                    Item icon = Items.LIGHT_GRAY_STAINED_GLASS_PANE;

                                                    if (claimed != null) {
                                                        if (claimed == existingNation)
                                                            icon = Items.GREEN_STAINED_GLASS_PANE;
                                                        else {
                                                            icon = Items.RED_STAINED_GLASS_PANE;
                                                        }
                                                    }

                                                    String nationName = claimed != null ? claimed.getName() : "Wilderness";
                                                    boolean glow = offset.equals(chunkPos);

                                                    GuiElementBuilder elementBuilder = new GuiElementBuilder()
                                                            .setItem(icon)
                                                            .setName(Component.literal(nationName + " (" + offset.x + "," + offset.z + ")"))
                                                            .setCallback((index2, type2, action2, gui2) -> {
                                                                if (claimed != enemyNation) {
                                                                    player.sendSystemMessage(Component.literal("Incorrect nation claim " + nationName
                                                                            + ", expected " + enemyNation.getName()));
                                                                } else {
                                                                    if (!TeamHandler.membersNearby(player.server, offset, existingNation)) {
                                                                        nationData.startSiege(existingNation, enemyNation, player.getLevel(), offset);
                                                                        gui2.close();
                                                                    } else {
                                                                        player.sendSystemMessage(Component.literal(
                                                                                "Can't start siege with nation members within 16 blocks of enemy claim"));
                                                                    }
                                                                }
                                                            });

                                                    if (glow) {
                                                        elementBuilder.glow();
                                                    }

                                                    siege2Gui.setSlot(i1, elementBuilder);
                                                    i1++;
                                                }
                                            }
                                            siege2Gui.open();
                                        } else {
                                            player.sendSystemMessage(Component.literal("Not enough members online to raid "+enemyNation.getName()));
                                        }
                                    })
                            );
                        }
                    }
                    raidGui.open();
                })
        );

        teamLeaderMenu.open();
    }

    static final UnaryOperator<Style> NO_ITALIC = style -> style.withItalic(false);

    static List<ServerPlayer> getUninvitedPlayers(ServerPlayer leader, Nation nation) {
        List<ServerPlayer> allPlayers = new ArrayList<>(leader.server.getPlayerList().getPlayers());
        allPlayers.removeIf(player -> Services.PLATFORM.getNation(player) != null);
        return allPlayers;
    }

    static List<GameProfile> getAllTeamMembersExceptLeader(ServerPlayer leader, Nation nation) {
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

    public static boolean onMovePacket(ServerGamePacketListenerImpl packetHandler, ServerPlayer player, ServerboundMovePlayerPacket packet) {
        NationData nationData = NationData.getNationInstance(player.getLevel());
        if (nationData != null) {
            Siege siege = nationData.getActiveSiege();
            if (siege != null) {
                if (siege.isAttacking(player) && siege.shouldBlockAttackers() && TeamHandler.isPlayerNearClaim(player, siege.getClaimPos())) {
                    if (packet.hasPosition()) {
                        player.sendSystemMessage(Component.literal("Can't move into enemy claim during start of siege"));
                        Vec3 newPos = getNearestLegalPosition(player.position(), siege.getClaimPos(), 1);
                        packetHandler.teleport(newPos.x, newPos.y, newPos.z, player.getYRot(), player.getXRot());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static Vec3 getNearestLegalPosition(Vec3 position, ChunkPos claim, int radius) {
        double playerX = position.x;
        double playerZ = position.z;

        int minLegalX = claim.getMinBlockX() - 16 * radius;
        int minLegalZ = claim.getMinBlockZ() - 16 * radius;

        int maxLegalX = claim.getMaxBlockX() + 16 * radius;
        int maxLegalZ = claim.getMaxBlockZ() + 16 * radius;

        double toMoveZ1 = maxLegalZ - playerZ;//distance to south border
        double toMoveX1 = maxLegalX - playerX;//distance to east border

        double toMoveZ2 = playerZ - minLegalZ;//distance to north border
        double toMoveX2 = playerX - minLegalX;//distance to west border

        TreeMap<Double, Direction> map = new TreeMap<>();
        map.put(toMoveZ1, Direction.SOUTH);
        map.put(toMoveX1, Direction.EAST);
        map.put(toMoveZ2, Direction.NORTH);
        map.put(toMoveX2, Direction.WEST);
        double y = position.y;

        Direction toMove = map.get(map.keySet().iterator().next());

        double backTeleport = .1;

        switch (toMove) {
            case NORTH -> {
                return new Vec3(playerX, y, minLegalZ - backTeleport);
            }
            case EAST -> {
                return new Vec3(maxLegalX + 1 + backTeleport, y, playerZ);
            }
            case SOUTH -> {
                return new Vec3(playerX, y, maxLegalZ + 1 + backTeleport);
            }
            case WEST -> {
                return new Vec3(minLegalX - backTeleport, y, playerZ);
            }
            default -> {
                return new Vec3(playerX, y, minLegalZ - backTeleport);
            }
        }
    }

}