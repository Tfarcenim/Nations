package tfar.nations;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import tfar.nations.level.OfflineTrackerData;
import tfar.nations.nation.Nation;
import tfar.nations.nation.NationData;
import tfar.nations.sgui.api.elements.GuiElement;
import tfar.nations.sgui.api.elements.GuiElementBuilder;
import tfar.nations.sgui.api.elements.GuiElementInterface;
import tfar.nations.sgui.api.gui.SimpleGui;

import java.util.*;

import static tfar.nations.Nations.*;

public class ServerButtons {

    public static GuiElementBuilder topNationsButton(ServerPlayer player, NationData nationData) {
        return new GuiElementBuilder(Items.NETHER_STAR)
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
                                .setName(Component.literal(nation.getName() + " - " + nation.getTotalPower() + " Power"))
                                .setCallback((index2, type2, action2, gui2) -> {
                                })
                        );
                    }
                    nationsTopGui.open();
                });
    }

    private static GuiElementBuilder exilePlayersButton(ServerPlayer player, NationData nationData, Nation existingNation) {
        return new GuiElementBuilder(Items.IRON_SWORD)
                .setName(Component.literal("Exile Members"))
                .hideFlags()
                .setCallback((index1, clickType1, actionType1) -> {
                    SimpleGui exileGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
                    exileGui.setTitle(Component.literal("Exile Members"));
                    List<GameProfile> members = Nations.getAllTeamMembersExceptLeader(player, existingNation);
                    int i = 0;
                    for (GameProfile gameProfile : members) {
                        GuiElementBuilder elementBuilder = new GuiElementBuilder(Items.PLAYER_HEAD);
                        String name = gameProfile.getName();
                        if (player.server.getPlayerList().getPlayer(gameProfile.getId()) == null) {
                            long ticks = OfflineTrackerData.getOrCreateDefaultInstance(player.server).ticksSinceOnline(player.server, gameProfile);
                            String time = OfflineTrackerData.formatTime(ticks);
                            name += " (Offline) + last seen";
                            if (ticks > -1) {
                                name +=  " "+ time + " ago";
                            } else {
                                name +=  " unknown";
                            }
                        }
                        exileGui.setSlot(i, elementBuilder
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
        return new GuiElementBuilder(Items.PAPER)
                .setName(Component.literal("Invite Players"))
                .setCallback((index1, clickType1, actionType1) -> {
                    SimpleGui inviteGui = new SimpleGui(MenuType.GENERIC_9x6, player, false);

                    inviteGui.setTitle(Component.literal("Invite Players"));
                    List<ServerPlayer> eligible = Nations.getUninvitedPlayers(player, nationData);
                    int i = 0;
                    for (ServerPlayer invitePlayer : eligible) {
                        GuiElementBuilder elementBuilder = new GuiElementBuilder();
                        inviteGui.setSlot(i, elementBuilder
                                .setItem(Items.PLAYER_HEAD)
                                .setSkullOwner(invitePlayer.getGameProfile(), player.server)
                                .setName(invitePlayer.getName())
                                .setCallback(
                                        (index2, type1, action1, gui) -> {
                                            nationData.sendInvites(List.of(invitePlayer.getGameProfile()), existingNation,player.server);
                                            gui.close();
                                        }));
                        i++;
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
        return new GuiElementBuilder(Items.LEAD)
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
                    confirmGui.setSlot(0, new GuiElementBuilder(YES)
                            .setName(Component.literal("Yes"))
                            .setCallback((index1, clickType1, actionType1) -> {
                                ServerPlayer serverPlayer = confirmGui.getPlayer();
                                nationData.leaveNation(List.of(serverPlayer));
                                serverPlayer.sendSystemMessage(Component.literal("Left Nation " + existingNation.getName()));
                                confirmGui.close();
                            })
                    );
                    confirmGui.setSlot(4, new GuiElementBuilder(NO)
                            .setName(Component.literal("No"))
                            .setCallback((index1, clickType1, actionType1) -> confirmGui.close())
                    );
                    confirmGui.open();
                });
    }

    public static GuiElementBuilder onlinePlayersButton(ServerPlayer player, NationData nationData) {
        return new GuiElementBuilder()
                .setItem(Items.BOOK)
                .setName(Component.literal("Online Players"))
                .setCallback((index, type, action) -> {
                    SimpleGui onlineGui = new SimpleGui(MenuType.GENERIC_9x6, player, false);
                    onlineGui.setTitle(Component.literal("Online Players"));
                    List<ServerPlayer> allPlayers = player.server.getPlayerList().getPlayers();

                    final int pages = (int) Math.ceil(allPlayers.size() / 45f);
                    if (pages > 1) {
                        int[] page = new int[]{0};
                        updateOnlinePage(onlineGui, page[0], allPlayers, nationData);

                        onlineGui.setSlot(45, new GuiElementBuilder(Items.ARROW)
                                .setName(Component.literal("left"))
                                .setCallback((index1, type1, action1, gui) -> {
                                    if (page[0] >= 1) {
                                        page[0]--;
                                        updateOnlinePage(onlineGui, page[0], allPlayers, nationData);
                                    }
                                })
                        );

                        onlineGui.setSlot(53, new GuiElementBuilder(Items.ARROW)
                                .setName(Component.literal("right"))
                                .setCallback((index1, type1, action1, gui) -> {
                                    if (page[0] < pages - 1) {
                                        page[0]++;
                                        updateOnlinePage(onlineGui, page[0], allPlayers, nationData);
                                    }
                                })
                        );

                    } else {
                        for (int i = 0; i < allPlayers.size(); i++) {
                            ServerPlayer serverPlayer = allPlayers.get(i);

                            Nation nation = nationData.getNationOf(serverPlayer);
                            String nationName = nation != null ? nation.getName() : "None";
                            String string = serverPlayer.getGameProfile().getName() + " | " + nationName;

                            onlineGui.setSlot(i, new GuiElementBuilder(Items.PLAYER_HEAD)
                                    .setSkullOwner(serverPlayer.getGameProfile(), player.server)
                                    .setName(Component.literal(string)));
                        }
                    }

                    onlineGui.open();
                });
    }

    private static void updateOnlinePage(SimpleGui simpleGui, int page, List<ServerPlayer> players,NationData nationData) {
        for (int i = 0; i < 45; i++) {
            int offsetSlot = page * 45 + i;
            if (offsetSlot < players.size()) {
                ServerPlayer offsetPlayer = players.get(offsetSlot);
                Nation nation = nationData.getNationOf(offsetPlayer);
                String nationName = nation != null ? nation.getName() : "None";
                String string = offsetPlayer.getGameProfile().getName() + " | " + nationName;

                simpleGui.setSlot(i, new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setSkullOwner(offsetPlayer.getGameProfile(), offsetPlayer.server)
                        .setName(Component.literal(string))
                );
            } else {
                simpleGui.setSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public static GuiElementBuilder claimChunksButton1(ServerPlayer player, NationData nationData, Nation existingNation) {
        return new GuiElementBuilder()
                .setItem(Items.WHITE_BANNER)
                .setName(Component.literal("Claim land v1"))
                .setCallback((index, type, action) -> {
                    SimpleGui claimGui = new SimpleGui(MenuType.GENERIC_9x6, player, false);
                    claimGui.setTitle(Component.literal("Claim land v1"));
                    ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                    int index1 = 0;
                    for (int z = -2; z < 4; z++) {
                        for (int x = -4; x < 5; x++) {
                            ChunkPos offset = new ChunkPos(chunkPos.x + x, chunkPos.z + z);

                            Nation claimed = nationData.getNationAtChunk(offset);
                            Item icon = Items.LIGHT_GRAY_STAINED_GLASS_PANE;

                            if (claimed != null) {
                                if (claimed == existingNation) icon = Items.GREEN_STAINED_GLASS_PANE;
                                else {
                                    icon = Items.RED_STAINED_GLASS_PANE;
                                }
                            }

                            String nationName = claimed != null ? claimed.getName() : "Wilderness";
                            boolean glow = offset.equals(chunkPos);

                            GuiElementBuilder elementBuilder = new GuiElementBuilder()
                                    .setItem(icon)
                                    .setName(Component.literal(nationName + " (" + offset.x + "," + offset.z + ")"))
                                    .setCallback((index2, type1, action1, gui) -> {
                                        GuiElementInterface slot = gui.getSlot(index2);
                                        ItemStack stack = slot.getItemStack();


                                        if (stack.is(Items.LIGHT_GRAY_STAINED_GLASS_PANE)) {

                                            boolean checkPower = existingNation.canClaim();
                                            if (checkPower) {
                                                ItemStack newClaim = new ItemStack(Items.GREEN_STAINED_GLASS_PANE);
                                                if (glow) {
                                                    newClaim.enchant(Enchantments.UNBREAKING, 0);
                                                    newClaim.getTag().putByte("HideFlags", (byte) ItemStack.TooltipPart.ENCHANTMENTS.getMask());
                                                }
                                                newClaim.setHoverName(Component.literal(existingNation.getName() + " (" + offset.x + "," + offset.z + ")")
                                                        .withStyle(NO_ITALIC));
                                                ((GuiElement) slot).setItemStack(newClaim);
                                                nationData.addClaim(existingNation, offset);
                                            } else {
                                                player.sendSystemMessage(Component.literal("Insufficient nation power"));
                                            }

                                        } else if (stack.is(Items.GREEN_STAINED_GLASS_PANE)) {
                                            ItemStack wilderness = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);

                                            if (glow) {
                                                wilderness.enchant(Enchantments.UNBREAKING, 0);
                                                wilderness.getTag().putByte("HideFlags", (byte) ItemStack.TooltipPart.ENCHANTMENTS.getMask());
                                            }

                                            nationData.removeClaim(existingNation, offset);
                                            wilderness.setHoverName(Component.literal("Wilderness (" + offset.x + "," + offset.z + ")").withStyle(NO_ITALIC));
                                            ((GuiElement) slot).setItemStack(wilderness);
                                        }
                                    });

                            if (glow) {
                                elementBuilder.glow();
                            }

                            claimGui.setSlot(index1, elementBuilder);
                            index1++;
                        }
                    }
                    claimGui.open();
                });
    }

    public static GuiElementBuilder unClaimChunksButton1(ServerPlayer player, NationData nationData, Nation existingNation) {
        return new GuiElementBuilder()
                .setItem(Items.WHITE_BANNER)
                .setName(Component.literal("Unclaim land"))
                .setCallback((index, type, action) -> {
                    SimpleGui claimGui = new SimpleGui(MenuType.GENERIC_9x6, player, false);
                    claimGui.setTitle(Component.literal("Unclaim land"));
                    ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                    int index1 = 0;
                    for (int z = -2; z < 4; z++) {
                        for (int x = -4; x < 5; x++) {
                            ChunkPos offset = new ChunkPos(chunkPos.x + x, chunkPos.z + z);

                            Nation claimed = nationData.getNationAtChunk(offset);
                            Item icon = Items.LIGHT_GRAY_STAINED_GLASS_PANE;

                            if (claimed != null) {
                                if (claimed == existingNation) icon = Items.GREEN_STAINED_GLASS_PANE;
                                else {
                                    icon = Items.RED_STAINED_GLASS_PANE;
                                }
                            }

                            String nationName = claimed != null ? claimed.getName() : "Wilderness";
                            boolean glow = offset.equals(chunkPos);

                            GuiElementBuilder elementBuilder = new GuiElementBuilder()
                                    .setItem(icon)
                                    .setName(Component.literal(nationName + " (" + offset.x + "," + offset.z + ")"))
                                    .setCallback((index2, type1, action1, gui) -> {
                                        GuiElementInterface slot = gui.getSlot(index2);
                                        ItemStack stack = slot.getItemStack();


                                        if (stack.is(Items.GREEN_STAINED_GLASS_PANE)) {
                                            ItemStack wilderness = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);

                                            if (glow) {
                                                wilderness.enchant(Enchantments.UNBREAKING, 0);
                                                wilderness.getTag().putByte("HideFlags", (byte) ItemStack.TooltipPart.ENCHANTMENTS.getMask());
                                            }

                                            nationData.removeClaim(existingNation, offset);
                                            wilderness.setHoverName(Component.literal("Wilderness (" + offset.x + "," + offset.z + ")").withStyle(NO_ITALIC));
                                            ((GuiElement) slot).setItemStack(wilderness);
                                        }
                                    });

                            if (glow) {
                                elementBuilder.glow();
                            }

                            claimGui.setSlot(index1, elementBuilder);
                            index1++;
                        }
                    }
                    claimGui.open();
                });
    }

    public static GuiElementBuilder claimChunksButton2(ServerPlayer player, NationData nationData, Nation existingNation) {
        return new GuiElementBuilder()
                .setItem(Items.WHITE_BANNER)
                .setName(Component.literal("Claim Land"))//v2
                .setCallback((index, type, action) -> {
                    SimpleGui claimGui = new SimpleGui(MenuType.GENERIC_9x6, player, false);
                    claimGui.setTitle(Component.literal("Claim Land"));//v2
                    ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                    int index1 = 0;
                    final Map<Integer,ChunkPos> slotmap = new HashMap<>();

                    for (int z = -2; z < 4; z++) {
                        for (int x = -4; x < 5; x++) {
                            ChunkPos offset = new ChunkPos(chunkPos.x + x, chunkPos.z + z);
                            slotmap.put(index1,offset);

                            Nation claimed = nationData.getNationAtChunk(offset);
                            Item icon = Items.LIGHT_GRAY_STAINED_GLASS_PANE;

                            if (claimed != null) {
                                if (claimed == existingNation) icon = Items.GREEN_STAINED_GLASS_PANE;
                                else {
                                    icon = Items.RED_STAINED_GLASS_PANE;
                                }
                            }

                            String nationName = claimed != null ? claimed.getName() : "Wilderness";
                            boolean glow = offset.equals(chunkPos);

                            GuiElementBuilder elementBuilder = new GuiElementBuilder()
                                    .setItem(icon)
                                    .setName(Component.literal(nationName + " (" + offset.x + "," + offset.z + ")"))
                                    .setCallback((index2, type1, action1, gui) -> {
                                        GuiElementInterface slot = claimGui.getSlot(index2);
                                        Nation checkNation = nationData.getNationAtChunk(offset);
                                        if (checkNation != null) {
                                            player.sendSystemMessage(Component.literal("There's already a claim at this chunk"),false);
                                        } else {
                                            int yellowpos = -1;
                                            for (int i = 0; i < 54;i++) {
                                                if (gui.getSlot(i).getItemStack().is(Items.YELLOW_STAINED_GLASS_PANE)) {
                                                    yellowpos = i;
                                                }
                                            }

                                            if (yellowpos > -1) {
                                                ChunkPos pos1 = slotmap.get(yellowpos);
                                                Set<ChunkPos> eligible = eligibleChunks(nationData, offset,pos1);
                                                if (eligible.isEmpty()) {
                                                    player.sendSystemMessage(Component.literal("Couldn't find any eligible chunks to claim"));
                                                } else {
                                                    int powerNeeded = eligible.size();
                                                    if (existingNation.canClaim(powerNeeded)) {
                                                        nationData.addClaims(existingNation,eligible);
                                                        gui.close();
                                                    } else {
                                                        player.sendSystemMessage(Component.literal("Insufficient nation power to claim "+
                                                                eligible.size() + " chunks"));
                                                    }
                                                }
                                            } else {
                                                ItemStack stack = Items.YELLOW_STAINED_GLASS_PANE.getDefaultInstance();
                                                stack.setTag(slot.getItemStack().getTag());
                                                ((GuiElement)slot).setItemStack(stack);
                                            }
                                        }
                                    });

                            if (glow) {
                                elementBuilder.glow();
                            }

                            claimGui.setSlot(index1, elementBuilder);
                            index1++;
                        }
                    }
                    claimGui.open();
                });
    }

    public static Set<ChunkPos> eligibleChunks(NationData nationData, ChunkPos pos1, ChunkPos pos2) {
        Set<ChunkPos> valid = new HashSet<>();

        int minx = Math.min(pos1.x,pos2.x);
        int minz = Math.min(pos1.z,pos2.z);

        int maxx = Math.max(pos1.x,pos2.x);
        int maxz = Math.max(pos1.z,pos2.z);

        for (int z = minz; z <= maxz;z++) {
            for (int x = minx; x <= maxx;x++) {
                ChunkPos pos = new ChunkPos(x,z);
                Nation nation = nationData.getNationAtChunk(pos);
                if (nation == null) valid.add(pos);
            }
        }

        return valid;
    }
}
