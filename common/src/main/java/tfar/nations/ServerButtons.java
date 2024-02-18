package tfar.nations;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import tfar.nations.nation.Nation;
import tfar.nations.nation.NationData;
import tfar.nations.sgui.api.elements.GuiElementBuilder;
import tfar.nations.sgui.api.gui.SimpleGui;

import java.util.Comparator;
import java.util.List;

import static tfar.nations.Nations.NO;
import static tfar.nations.Nations.YES;

public class ServerButtons {

    public static GuiElementBuilder topNationsButton(ServerPlayer player, NationData nationData) {
        return new GuiElementBuilder()
                .setItem(Items.NETHER_STAR)
                .hideFlags()
                .setName(Component.literal("Top Nations"))
                .setCallback((index1, type1, action1, gui1) -> {
                    SimpleGui nationsTopGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
                    nationsTopGui.setTitle(Component.literal("Top Nations"));
                    List<Nation> nations = nationData.getNations();
                    nations.sort(Comparator.comparingInt(Nation::getTotalPower));
                    for (int i = 0; i < nations.size(); i++) {
                        Nation nation = nations.get(i);
                        nationsTopGui.setSlot(i, new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setSkullOwner(nation.getOwner(), player.server)
                                .setName(Component.literal(nation.getOwner().getName() + " - " + nation.getTotalPower() + " Power"))
                                .setCallback((index2, type2, action2, gui2) -> {
                                })
                        );
                    }
                    nationsTopGui.open();
                });
    }

    private static GuiElementBuilder exilePlayersButton(ServerPlayer player, NationData nationData, Nation existingNation) {
        return new GuiElementBuilder()
                .setItem(Items.IRON_SWORD)
                .setName(Component.literal("Exile Members"))
                .hideFlags()
                .setCallback((index1, clickType1, actionType1) -> {
                    SimpleGui exileGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
                    exileGui.setTitle(Component.literal("Exile Members"));
                    List<GameProfile> members = Nations.getAllTeamMembersExceptLeader(player, existingNation);
                    int i = 0;
                    for (GameProfile gameProfile : members) {
                        GuiElementBuilder elementBuilder = new GuiElementBuilder();
                        String name = gameProfile.getName();
                        if (player.server.getPlayerList().getPlayer(gameProfile.getId()) == null) {
                            name += " (Offline)";
                        }
                        exileGui.setSlot(i, elementBuilder
                                .setItem(Items.PLAYER_HEAD)
                                .setSkullOwner(gameProfile, player.server)
                                .setName(Component.literal(name))
                                .setCallback(
                                        (index2, type1, action1, gui) -> {
                                            nationData.leaveNationGameProfiles(player.server, List.of(gameProfile));
                                            player.sendSystemMessage(Component.literal(gameProfile.getName() + " has been exiled"));
                                            gui.close();
                                        }));
                    }

                    exileGui.open();
                });
    }

    private static GuiElementBuilder invitePlayersButton(ServerPlayer player, NationData nationData, Nation existingNation) {
        return new GuiElementBuilder()
                .setItem(Items.PAPER)
                .setName(Component.literal("Invite Players"))
                .setCallback((index1, clickType1, actionType1) -> {
                    SimpleGui inviteGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);

                    inviteGui.setTitle(Component.literal("Invite Players"));
                    List<ServerPlayer> eligible = Nations.getUninvitedPlayers(player, existingNation);
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
                });
    }

    public static GuiElementBuilder managePlayersButton(ServerPlayer officer, NationData nationData, Nation existingNation) {
        return new GuiElementBuilder()
                .setItem(Items.LEAD)
                .setName(Component.literal("Manage Players"))
                .setCallback((index, type, action) -> {
                    SimpleGui managePlayers = new SimpleGui(MenuType.HOPPER, officer, false);
                    managePlayers.setTitle(Component.literal("Manage Players"));

                    managePlayers.setSlot(0, ServerButtons.invitePlayersButton(officer, nationData, existingNation));
                    managePlayers.setSlot(1, ServerButtons.exilePlayersButton(officer, nationData, existingNation));
                    managePlayers.setSlot(2, ServerButtons.promotePlayersButton(officer, nationData, existingNation));
                    managePlayers.setSlot(3, ServerButtons.demotePlayersButton(officer, nationData, existingNation));

                    managePlayers.open();
                });
    }

    private static GuiElementBuilder promotePlayersButton(ServerPlayer officer, NationData nationData, Nation existingNation) {
        return new GuiElementBuilder()
                .setItem(Items.GOLDEN_SWORD)
                .setName(Component.literal("Promote Members"))
                .hideFlags()
                .setCallback((index1, clickType1, actionType1) -> {
                    SimpleGui promoteGui = new SimpleGui(MenuType.GENERIC_9x3, officer, false);
                    promoteGui.setTitle(Component.literal("Promote Members"));
                    List<GameProfile> members = existingNation.getPromotable(officer);
                    int i = 0;
                    for (GameProfile gameProfile : members) {
                        GuiElementBuilder elementBuilder = new GuiElementBuilder();
                        String name = gameProfile.getName();
                        if (officer.server.getPlayerList().getPlayer(gameProfile.getId()) == null) {
                            name += " (Offline)";
                        }

                        int rank = existingNation.getRank(gameProfile);
                        name += " | Rank " + rank;

                        promoteGui.setSlot(i, elementBuilder
                                .setItem(Items.PLAYER_HEAD)
                                .setSkullOwner(gameProfile, officer.server)
                                .setName(Component.literal(name))
                                .setCallback(
                                        (index2, type1, action1, gui) -> {
                                            nationData.promote(gameProfile, existingNation);
                                            officer.sendSystemMessage(Component.literal(gameProfile.getName() + " has been promoted"));
                                            gui.close();
                                        }));
                    }

                    promoteGui.open();
                });
    }

    private static GuiElementBuilder demotePlayersButton(ServerPlayer officer, NationData nationData, Nation existingNation) {
        return new GuiElementBuilder()
                .setItem(Items.STONE_SWORD)
                .setName(Component.literal("Demote Members"))
                .hideFlags()
                .setCallback((index1, clickType1, actionType1) -> {
                    SimpleGui demoteGui = new SimpleGui(MenuType.GENERIC_9x3, officer, false);
                    demoteGui.setTitle(Component.literal("Demote Members"));
                    List<GameProfile> members = existingNation.getPromotable(officer);
                    int i = 0;
                    for (GameProfile gameProfile : members) {
                        GuiElementBuilder elementBuilder = new GuiElementBuilder();
                        String name = gameProfile.getName();
                        if (officer.server.getPlayerList().getPlayer(gameProfile.getId()) == null) {
                            name += " (Offline)";
                        }

                        int rank = existingNation.getRank(gameProfile);
                        name += " | Rank " + rank;

                        demoteGui.setSlot(i, elementBuilder
                                .setItem(Items.PLAYER_HEAD)
                                .setSkullOwner(gameProfile, officer.server)
                                .setName(Component.literal(name))
                                .setCallback(
                                        (index2, type1, action1, gui) -> {
                                            nationData.demote(gameProfile, existingNation);
                                            officer.sendSystemMessage(Component.literal(gameProfile.getName() + " has been demoted"));
                                            gui.close();
                                        }));
                    }

                    demoteGui.open();
                });
    }

    public static GuiElementBuilder leaveTeamButton(ServerPlayer player, NationData nationData, Nation existingNation) {
        return new GuiElementBuilder()
                .setItem(Items.BARRIER)
                .setName(Component.literal("Leave Nation"))
                .setCallback((index, type, action) -> {
                    SimpleGui confirmGui = new SimpleGui(MenuType.HOPPER, player, false);
                    confirmGui.setTitle(Component.literal("Leave Nation?"));
                    confirmGui.setSlot(0, new GuiElementBuilder()
                            .setItem(YES)
                            .setName(Component.literal("Yes"))
                            .setCallback((index1, clickType1, actionType1) -> {
                                ServerPlayer serverPlayer = confirmGui.getPlayer();
                                nationData.leaveNation(List.of(serverPlayer));
                                serverPlayer.sendSystemMessage(Component.literal("Left Nation " + existingNation.getName()));
                                confirmGui.close();
                            })
                    );
                    confirmGui.setSlot(4, new GuiElementBuilder()
                            .setItem(NO)
                            .setName(Component.literal("No"))
                            .setCallback((index1, clickType1, actionType1) -> confirmGui.close())
                    );
                    confirmGui.open();
                });
    }

}
