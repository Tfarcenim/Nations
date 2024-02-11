package tfar.nations.nation;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.PlayerDataStorage;
import tfar.nations.mixin.MinecraftServerAccessor;
import tfar.nations.platform.Services;

import java.util.*;

public class Nation {

    private String name;
    private final Set<GameProfile> members = new HashSet<>();
    private final Set<ChunkPos> claimed = new HashSet<>();
    private int color =0xffffff;
    private GameProfile owner;
    private final Set<String> allies = new HashSet<>();
    private final Set<String> enemies = new HashSet<>();

    public int getTotalPower() {
        return 5 * (members.size() + 1);
    }

    public Set<ChunkPos> getClaimed() {
        return claimed;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void addPlayers(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            GameProfile gameProfile = player.getGameProfile();
            members.add(gameProfile);
            Services.PLATFORM.setNation(player,this);
        }
    }

    public void setOwner(ServerPlayer newOwner) {
        this.owner = newOwner.getGameProfile();
        addPlayers(List.of(newOwner));
    }

    public GameProfile getOwner() {
        return owner;
    }

    public boolean canClaim() {
        return claimed.size() < getTotalPower();
    }

    public boolean isOwner(ServerPlayer player) {
        return player.getGameProfile().equals(owner);
    }

    public void removePlayers(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            GameProfile gameProfile = player.getGameProfile();
            members.remove(gameProfile);
            Services.PLATFORM.setNation(player,null);
        }
    }

    public void removeGameProfiles(MinecraftServer server, Collection<GameProfile> gameProfiles) {
        for (GameProfile gameProfile : gameProfiles) {
            ServerPlayer player = server.getPlayerList().getPlayer(gameProfile.getId());
            if (player != null) {
                player.sendSystemMessage(Component.literal("You have been exiled from "+name));
                 Services.PLATFORM.setNation(player,null);
            } else {
                PlayerDataStorage playerDataStorage = ((MinecraftServerAccessor)server).getPlayerDataStorage();
                ServerPlayer fakePlayer = Services.PLATFORM.getFakePlayer(server.overworld(),gameProfile);
                CompoundTag nbt = playerDataStorage.load(fakePlayer);
                if (nbt != null) {
                    Services.PLATFORM.setNation(fakePlayer,null);
                    playerDataStorage.save(fakePlayer);
                }
            }
            members.remove(gameProfile);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public CompoundTag save() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("name",name);
        ListTag listTag = new ListTag();
        for (GameProfile gameProfile : members) {
            listTag.add(NbtUtils.writeGameProfile(new CompoundTag(),gameProfile));
        }
        compoundTag.put("members",listTag);
        compoundTag.putInt("color",color);
        compoundTag.put("owner",NbtUtils.writeGameProfile(new CompoundTag(),owner));
        ListTag claimedTag = new ListTag();
        for (ChunkPos chunkPos : claimed) {
            CompoundTag compound = new CompoundTag();
            compound.putInt("x",chunkPos.x);
            compound.putInt("z",chunkPos.z);
            claimedTag.add(compound);
        }
        compoundTag.put("claimed",claimedTag);

        ListTag alliedTag = new ListTag();
        for (String string : allies) {
            StringTag stringTag = StringTag.valueOf(string);
            alliedTag.add(stringTag);
        }
        compoundTag.put("allies",alliedTag);

        ListTag enemyTag = new ListTag();
        for (String string : enemies) {
            StringTag stringTag = StringTag.valueOf(string);
            enemyTag.add(stringTag);
        }
        compoundTag.put("enemies",enemyTag);

        return compoundTag;
    }

    public Set<GameProfile> getMembers() {
        return members;
    }

    public static Nation loadStatic(CompoundTag tag) {
        Nation nation = new Nation();
        nation.name = tag.getString("name");
        ListTag listTag = tag.getList("members", Tag.TAG_COMPOUND);
        for (Tag tag1 : listTag) {
            CompoundTag stringTag = (CompoundTag) tag1;
            nation.members.add(NbtUtils.readGameProfile(stringTag));
        }
        nation.color = tag.getInt("color");
        nation.owner = NbtUtils.readGameProfile(tag.getCompound("owner"));
        ListTag claimedTag = tag.getList("claimed",Tag.TAG_COMPOUND);
        for (Tag tag1 : claimedTag) {
            CompoundTag compound = (CompoundTag) tag1;
            nation.claimed.add(new ChunkPos(compound.getInt("x"),compound.getInt("z")));
        }
        ListTag alliedTag = tag.getList("allies",Tag.TAG_STRING);
        for (Tag tag1 : alliedTag) {
            StringTag stringTag = (StringTag) tag1;
            nation.allies.add(stringTag.getAsString());
        }
        ListTag enemiesTag = tag.getList("enemies",Tag.TAG_STRING);
        for (Tag tag1 : enemiesTag) {
            StringTag stringTag = (StringTag) tag1;
            nation.allies.add(stringTag.getAsString());
        }
        return nation;
    }
}
