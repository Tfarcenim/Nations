package tfar.nations.nation;

import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import tfar.nations.Nations;

import java.util.*;

public class NationData extends SavedData {

    private final List<Nation> nations = new ArrayList<>();
    private final Map<String,Nation> nationsLookup = new HashMap<>();
    private final Map<UUID,Nation> invites = new HashMap<>();

    public static NationData getNationInstance(ServerLevel serverLevel) {
        return serverLevel.getDataStorage()
                .computeIfAbsent(NationData::loadStatic, NationData::new, Nations.MOD_ID);
    }

    public static NationData getDefaultNationsInstance(MinecraftServer server) {
        return getNationInstance(server.getLevel(Level.OVERWORLD));
    }

    public static NationData loadStatic(CompoundTag compoundTag) {
        NationData id = new NationData();
        id.load(compoundTag);
        return id;
    }

    public Nation createNation(String name) {
        if (nationsLookup.get(name) != null) return null;
        Nation nation = new Nation();
        nation.setName(name);
        nations.add(nation);
        nationsLookup.put(name,nation);
        setDirty();
        return nation;
    }

    public void sendInvites(List<GameProfile> profiles,Nation nation) {
        for (GameProfile gameProfile : profiles) {
            invites.put(gameProfile.getId(),nation);
        }
        setDirty();
    }

    public Nation getInviteForPlayer(ServerPlayer player) {
        return invites.get(player.getUUID());
    }

    public void removeInvite(ServerPlayer player) {
        invites.remove(player.getUUID());
        setDirty();
    }

    public boolean removeNation(String name) {
        Nation toRemove = nationsLookup.get(name);
        boolean b = nations.remove(toRemove);
        nationsLookup.remove(name);
        setDirty();
        return b;
    }

    public List<Nation> getNations() {
        return nations;
    }

    public Nation getNationByName(String name) {
        return nationsLookup.get(name);
    }

    public void load(CompoundTag tag) {
        nations.clear();
        nationsLookup.clear();
        ListTag listTag = tag.getList(Nations.MOD_ID, Tag.TAG_COMPOUND);
        for (Tag tag1 : listTag) {
            CompoundTag compoundTag = (CompoundTag) tag1;
            Nation nation = Nation.loadStatic(compoundTag);
            nations.add(nation);
            nationsLookup.put(nation.getName(),nation);
        }
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        ListTag listTag = new ListTag();
        for (Nation nation : nations) {
            listTag.add(nation.save());
        }
        pCompoundTag.put(Nations.MOD_ID, listTag);
        return pCompoundTag;
    }


    public boolean joinNation(String name, Collection<ServerPlayer> serverPlayers) {
        Nation nation = getNationByName(name);
        if (nation != null) {
            nation.addPlayers(serverPlayers);
            setDirty();
            return true;
        }
        return false;
    }

    public boolean setOwner(String name,ServerPlayer player) {
        Nation nation = getNationByName(name);
        if (nation != null) {
            nation.setOwner(player);
            setDirty();
        }
        return false;
    }

    public boolean leaveNation(Collection<ServerPlayer> serverPlayers) {
        for (Nation nation : nations) {
            nation.removePlayers(serverPlayers);
        }
        setDirty();
        return true;
    }
}
