package tfar.nations.nation;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.jetbrains.annotations.Nullable;
import tfar.nations.Nations;
import tfar.nations.TeamHandler;
import tfar.nations.level.OfflineTrackerData;
import tfar.nations.mixin.MinecraftServerAccessor;
import tfar.nations.platform.Services;

import java.util.*;
import java.util.stream.Collectors;

public class Nation {

    private String name;
    private final Object2IntMap<GameProfile> members = new Object2IntOpenHashMap<>();
    private final Set<ChunkPos> claimed = new HashSet<>();
    private int color = 0xffffff;
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

    public Set<String> getAllies() {
        return allies;
    }

    public Set<String> getEnemies() {
        return enemies;
    }

    public List<ServerPlayer> getOnlineMembers(MinecraftServer server) {
        return members.keySet().stream().map(gameProfile -> server.getPlayerList().getPlayer(gameProfile.getId())).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public boolean isAlly(@Nullable Nation other) {
        return other != null && allies.contains(other.name);
    }

    public boolean isEnemy(@Nullable Nation other) {
        return other != null && enemies.contains(other.name);
    }

    public List<GameProfile> getPromotable(ServerPlayer promoter) {
        List<GameProfile> promotable = new ArrayList<>(members.keySet());
        promotable.remove(owner);//remove owner from promotion
        if (!isOwner(promoter)) {
            GameProfile promoterProfile = promoter.getGameProfile();
            promotable.remove(promoterProfile);//remove self from promotion
            int rank = members.getInt(promoterProfile);
            promotable.removeIf(profile -> rank <= members.getInt(profile));
        }
        return promotable;
    }

    public boolean tick(MinecraftServer server) {
        List<GameProfile> toKick = new ArrayList<>();
        for (GameProfile profile : getMembers()) {
            if (profile.equals(owner)) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(profile.getId());
            if (player != null) continue;
            long time = OfflineTrackerData.getOrCreateDefaultInstance(server).ticksSinceOnline(server,profile);
            if (time > NationData.KICK_TIME) {
                toKick.add(profile);
                ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(owner.getId());
                if (ownerPlayer != null)
                    ownerPlayer.sendSystemMessage(Component.literal("Kicked "+ profile.getName() + " from nation for inactivity"));
                }
            }
        removeGameProfiles(server,toKick);
        return !toKick.isEmpty();
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
            members.put(gameProfile, 0);
            Services.PLATFORM.setNation(player, this);
            TeamHandler.updateOthers(player);
            TeamHandler.updateSelf(player);
        }
    }

    public void setOwner(ServerPlayer newOwner) {
        this.owner = newOwner.getGameProfile();
        addPlayers(List.of(newOwner));
        members.put(owner, 2);
    }

    public boolean isOfficer(ServerPlayer player) {
        return members.getInt(player.getGameProfile()) > 0;
    }

    public int getRank(GameProfile profile) {
        return members.getInt(profile);
    }

    public void demote(GameProfile profile) {
        if (members.containsKey(profile)) {
            members.put(profile, members.getInt(profile) - 1);
        }
    }

    public void promote(GameProfile profile) {
        if (members.containsKey(profile)) {
            members.put(profile, members.getInt(profile) + 1);
        }
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
            members.removeInt(gameProfile);
            Services.PLATFORM.setNation(player, null);
            TeamHandler.updateOthers(player);
            TeamHandler.removeAllTeams(player);
        }
    }

    public void removeGameProfiles(MinecraftServer server, Collection<GameProfile> gameProfiles) {
        for (GameProfile gameProfile : gameProfiles) {
            ServerPlayer player = server.getPlayerList().getPlayer(gameProfile.getId());
            if (player != null) {
                player.sendSystemMessage(Component.literal("You have been exiled from " + name));
                Services.PLATFORM.setNation(player, null);
                TeamHandler.removeAllTeams(player);
            } else {
                PlayerDataStorage playerDataStorage = ((MinecraftServerAccessor) server).getPlayerDataStorage();
                ServerPlayer fakePlayer = Services.PLATFORM.getFakePlayer(server.overworld(), gameProfile);
                CompoundTag nbt = playerDataStorage.load(fakePlayer);
                if (nbt != null) {
                    Services.PLATFORM.setNation(fakePlayer, null);
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
        compoundTag.putString("name", name);
        ListTag listTag = new ListTag();
        for (Object2IntMap.Entry<GameProfile> entry : members.object2IntEntrySet()) {
            CompoundTag tag = new CompoundTag();
            NbtUtils.writeGameProfile(tag, entry.getKey());
            tag.putInt("rank", entry.getIntValue());
            listTag.add(tag);
        }
        compoundTag.put("members", listTag);

        compoundTag.putInt("color", color);
        compoundTag.put("owner", NbtUtils.writeGameProfile(new CompoundTag(), owner));
        ListTag claimedTag = new ListTag();
        for (ChunkPos chunkPos : claimed) {
            CompoundTag compound = new CompoundTag();
            compound.putInt("x", chunkPos.x);
            compound.putInt("z", chunkPos.z);
            claimedTag.add(compound);
        }
        compoundTag.put("claimed", claimedTag);

        ListTag alliedTag = new ListTag();
        for (String string : allies) {
            StringTag stringTag = StringTag.valueOf(string);
            alliedTag.add(stringTag);
        }
        compoundTag.put("allies", alliedTag);

        ListTag enemyTag = new ListTag();
        for (String string : enemies) {
            StringTag stringTag = StringTag.valueOf(string);
            enemyTag.add(stringTag);
        }
        compoundTag.put("enemies", enemyTag);

        return compoundTag;
    }

    public Set<GameProfile> getMembers() {
        return members.keySet();
    }

    public static Nation loadStatic(CompoundTag tag) {
        Nation nation = new Nation();
        nation.name = tag.getString("name");
        ListTag listTag = tag.getList("members", Tag.TAG_COMPOUND);
        for (Tag tag1 : listTag) {
            CompoundTag stringTag = (CompoundTag) tag1;
            int rank = stringTag.getInt("rank");
            nation.members.put(NbtUtils.readGameProfile(stringTag), rank);
        }

        nation.color = tag.getInt("color");
        nation.owner = NbtUtils.readGameProfile(tag.getCompound("owner"));
        ListTag claimedTag = tag.getList("claimed", Tag.TAG_COMPOUND);
        for (Tag tag1 : claimedTag) {
            CompoundTag compound = (CompoundTag) tag1;
            nation.claimed.add(new ChunkPos(compound.getInt("x"), compound.getInt("z")));
        }
        ListTag alliedTag = tag.getList("allies", Tag.TAG_STRING);
        for (Tag tag1 : alliedTag) {
            StringTag stringTag = (StringTag) tag1;
            nation.allies.add(stringTag.getAsString());
        }
        ListTag enemiesTag = tag.getList("enemies", Tag.TAG_STRING);
        for (Tag tag1 : enemiesTag) {
            StringTag stringTag = (StringTag) tag1;
            nation.enemies.add(stringTag.getAsString());
        }
        return nation;
    }
}
