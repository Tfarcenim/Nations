package tfar.nations;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import tfar.nations.nation.Nation;
import tfar.nations.nation.NationData;

import java.util.*;
import java.util.stream.Collectors;

public class TeamHandler {

    public static void updateSelf(ServerPlayer to, NationData nationData) {
        MinecraftServer server = to.server;
        Nation aboutNation = nationData.getNationOf(to);
        if (aboutNation == null) return;//this player has no colors
        List<ServerPlayer> friendly = new ArrayList<>();
        List<ServerPlayer> enemy = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == to) {
                continue;
            } else {
                Nation nation = nationData.getNationOf(player);
                if (nation == null) continue;//skip players that don't have a nation
                if (nation == aboutNation) {
                    friendly.add(player);
                } else if (aboutNation.isAlly(nation)) {
                    friendly.add(player);
                }
                else if (aboutNation.isEnemy(nation)) {
                    enemy.add(player);
                }
            }
        }
        mark(to,friendly,enemy);
    }

    public static void removeAllTeams(ServerPlayer to,NationData nationData) {
        Scoreboard dummy = new Scoreboard();

        PlayerTeam friends = new PlayerTeam(dummy,"friends");
        friends.setColor(ChatFormatting.GREEN);

        PlayerTeam enemies = new PlayerTeam(dummy,"enemies");
        to.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(friends));//create the team
        to.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(enemies));//create the team
        updateOthers(to,nationData);
    }

    public static void updateAll(ServerPlayer player,NationData nationData) {
        updateSelf(player, nationData);
        updateOthers(player, nationData);
    }

    public static void updateOthers(ServerPlayer player,NationData nationData) {
        List<ServerPlayer> players = new ArrayList<>(player.server.getPlayerList().getPlayers());
        players.remove(player);
        TeamHandler.updateOnlinePlayers(player,players,nationData);
    }

    private static void updateOnlinePlayers(ServerPlayer about,List<ServerPlayer> others,NationData nationData) {
        Scoreboard dummy = new Scoreboard();

        PlayerTeam friends = new PlayerTeam(dummy,"friends");
        friends.setColor(ChatFormatting.GREEN);

        PlayerTeam enemies = new PlayerTeam(dummy,"enemies");
        enemies.setColor(ChatFormatting.RED);


        Nation aboutNation = nationData.getNationOf(about);
        if (aboutNation == null) {
            for (ServerPlayer other : others) {
                other.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(friends, about.getGameProfile().getName(),
                        ClientboundSetPlayerTeamPacket.Action.REMOVE));//add the player to the team
                other.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(enemies, about.getGameProfile().getName(),
                        ClientboundSetPlayerTeamPacket.Action.REMOVE));//add the player to the team
            }
            return;
        }


        for (ServerPlayer other : others) {
            Nation nation = nationData.getNationOf(other);
            if (nation == null) continue;//skip players that don't have a nation
            PlayerTeam sendTeam = null;
            if (nation == aboutNation) {
                sendTeam = friends;
            } else if (aboutNation.isAlly(nation)) {
                sendTeam = friends;
            }
            else if (aboutNation.isEnemy(nation)) {
                sendTeam = enemies;
            }
            if (sendTeam != null) {
                other.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(sendTeam, about.getGameProfile().getName(),
                        ClientboundSetPlayerTeamPacket.Action.ADD));//add the player to the team
            } else {
                other.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(friends, about.getGameProfile().getName(),
                        ClientboundSetPlayerTeamPacket.Action.REMOVE));//add the player to the team
                other.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(enemies, about.getGameProfile().getName(),
                        ClientboundSetPlayerTeamPacket.Action.REMOVE));//add the player to the team
            }
        }
    }

    public static void sendMessageToTeam(MinecraftServer server,Component component,Nation nation) {
        Set<GameProfile> profiles = nation.getMembers();
        for (GameProfile gameProfile : profiles) {
            ServerPlayer player = server.getPlayerList().getPlayer(gameProfile.getId());
            if (player != null) {
                player.sendSystemMessage(component);
            }
        }
    }

    private static void mark(ServerPlayer to,List<ServerPlayer> friendly,List<ServerPlayer> enemy) {
        Scoreboard dummy = new Scoreboard();

        PlayerTeam friends = new PlayerTeam(dummy,"friends");
        friends.setColor(ChatFormatting.GREEN);
        friendly.forEach(player -> friends.getPlayers().add(player.getGameProfile().getName()));

        PlayerTeam enemies = new PlayerTeam(dummy,"enemies");
        enemies.setColor(ChatFormatting.RED);
        enemy.forEach(player -> enemies.getPlayers().add(player.getGameProfile().getName()));


        to.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(friends, true));//create the team
        to.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(enemies, true));//create the team

    }

    private static void markAsFriendly(ServerPlayer about,ServerPlayer message) {
        Scoreboard dummy = new Scoreboard();
        PlayerTeam pTeam = new PlayerTeam(dummy,"friendly");
        pTeam.setColor(ChatFormatting.GREEN);

        message.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(pTeam, true));//create the team
        message.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(pTeam, about.getGameProfile().getName(),
                ClientboundSetPlayerTeamPacket.Action.ADD));//add the player to the team
    }

    private static void markAsEnemy(ServerPlayer about,ServerPlayer message) {
        Scoreboard dummy = new Scoreboard();
        PlayerTeam pTeam = new PlayerTeam(dummy,"enemy");
        pTeam.setColor(ChatFormatting.RED);

        message.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(pTeam, true));//create the team
        message.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(pTeam, about.getGameProfile().getName(),
                ClientboundSetPlayerTeamPacket.Action.ADD));//add the player to the team
    }

    public static List<ServerPlayer> getOnlinePlayers(Collection<GameProfile> profiles,MinecraftServer server) {
        return profiles.stream().map(profile -> server.getPlayerList().getPlayer(profile.getId())).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static boolean membersNearby(MinecraftServer server,ChunkPos pos, Nation nation) {
        Set<GameProfile> members = nation.getMembers();
        for (GameProfile gameProfile : members) {
            ServerPlayer player = server.getPlayerList().getPlayer(gameProfile.getId());
            if (player != null) {
                if (isPlayerNearClaim(player,pos)) return true;
            }
        }
        return false;
    }

    public static boolean isPlayerNearClaim(ServerPlayer player,ChunkPos pos) {
        return player.level.dimension() == Level.OVERWORLD &&isPlayerInArea(player,pos,1);
    }

    public static boolean isPlayerInArea(ServerPlayer player,ChunkPos pos,int radius) {
        ChunkPos playerPos = new ChunkPos(player.blockPosition());
        return Math.abs(playerPos.x - pos.x) <= radius && Math.abs(playerPos.z - pos.z) <= radius;
    }

    public static boolean isPointInArea(Vec3 position,ChunkPos pos,int radius) {
        int check = 8 + radius * 16;
        return Math.abs(position.x - pos.getMiddleBlockX()) <= check && Math.abs(position.z - pos.getMiddleBlockZ()) <= check;
    }

}
