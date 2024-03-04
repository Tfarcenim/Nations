package tfar.nations.nation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import tfar.nations.platform.Services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteTeams {

    private static final Map<String, Scoreboard> remoteTeams = new HashMap<>();

    public static void syncFromNations(NationData nationData) {
        remoteTeams.clear();
        MinecraftServer server = Services.PLATFORM.getStaticServer();
        List<Nation> nations = nationData.getNations();
        List<ServerPlayer> allPlayers = server.getPlayerList().getPlayers();
        for (Nation nation : nations) {
            Scoreboard scoreboard = new Scoreboard();
            PlayerTeam friends = scoreboard.addPlayerTeam("friends");
            friends.setColor(ChatFormatting.GREEN);

            PlayerTeam otherFriends = scoreboard.addPlayerTeam("other_friends");
            otherFriends.setColor(ChatFormatting.LIGHT_PURPLE);

            PlayerTeam enemies = scoreboard.addPlayerTeam("enemies");
            enemies.setColor(ChatFormatting.RED);


            for (ServerPlayer player : allPlayers) {
                Nation playerNation = nationData.getNationOf(player);
                if (nation == null) continue;
                PlayerTeam addTo = null;
                if (playerNation == nation) {
                    addTo = friends;
                } else {
                    if (nation.isAlly(playerNation)) {
                        addTo = otherFriends;
                    }
                    if (nation.isEnemy(playerNation)) {
                        addTo = enemies;
                    }
                }
                if (addTo != null) {
                    scoreboard.addPlayerToTeam(player.getGameProfile().getName(),addTo);
                }
            }
            remoteTeams.put(nation.getName(),scoreboard);
        }
    }

    public static void syncToAllClients(MinecraftServer server) {
        List<ServerPlayer> allPlayers = server.getPlayerList().getPlayers();
        NationData nationData = NationData.getOrCreateDefaultNationsInstance(server);
        for (ServerPlayer player : allPlayers) {
            syncToOnly(player,nationData);
        }
    }

    public static void syncToOnly(ServerPlayer player,NationData nationData) {
        Nation nation = nationData.getNationOf(player);
        if (nation != null) {

            PlayerTeam dummyFriends = new PlayerTeam(null,"friends");
            PlayerTeam dummyOtherFriends = new PlayerTeam(null,"other_friends");
            PlayerTeam dummyEnemies = new PlayerTeam(null,"enemies");
            player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(dummyFriends));
            player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(dummyOtherFriends));
            player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(dummyEnemies));

            Scoreboard scoreboard = remoteTeams.get(nation.getName());
            for (PlayerTeam playerTeam : scoreboard.getPlayerTeams()) {
                player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam,true));
            }
        } else {
            PlayerTeam dummyFriends = new PlayerTeam(null,"friends");
            PlayerTeam dummyOtherFriends = new PlayerTeam(null,"other_friends");
            PlayerTeam dummyEnemies = new PlayerTeam(null,"enemies");
            player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(dummyFriends));
            player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(dummyOtherFriends));
            player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(dummyEnemies));
        }
    }
}
