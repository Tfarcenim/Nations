package tfar.nations;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tfar.nations.nation.Nation;

import java.util.Set;

public class TeamHandler {

    public static void sendMessageToTeam(MinecraftServer server,Component component,Nation nation) {
        Set<GameProfile> profiles = nation.getMembers();
        for (GameProfile gameProfile : profiles) {
            ServerPlayer player = server.getPlayerList().getPlayer(gameProfile.getId());
            if (player != null) {
                player.sendSystemMessage(component);
            }
        }
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
