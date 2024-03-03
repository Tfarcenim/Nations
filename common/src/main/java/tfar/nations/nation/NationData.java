package tfar.nations.nation;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import tfar.nations.Nations;
import tfar.nations.Siege;
import tfar.nations.TeamHandler;

import java.util.*;
import java.util.stream.Collectors;

public class NationData extends SavedData {

    private final List<Nation> nations = new ArrayList<>();
    private final Map<String,Nation> nationsLookup = new HashMap<>();
    private final Map<ChunkPos,Nation> chunkLookup = new HashMap<>();
    private final Map<GameProfile,Nation> playerLookup = new HashMap<>();

    private final Map<UUID,Nation> invites = new HashMap<>();
    private final Map<Nation,Nation> allyInvites = new HashMap<>();

    @Nullable
    private Siege activeSiege;

    public static final long KICK_TIME = 20 * 60 * 60 * 24 * 4;//4 irl days

    @Nullable
    public static NationData getNationInstance(ServerLevel serverLevel) {
        return serverLevel.getDataStorage()
                .get(compoundTag -> loadStatic(compoundTag, serverLevel),Nations.MOD_ID);
    }


    public static NationData getOrCreateNationInstance(ServerLevel serverLevel) {
        return serverLevel.getDataStorage()
                .computeIfAbsent(compoundTag -> loadStatic(compoundTag,serverLevel), NationData::new, Nations.MOD_ID);
    }

    public static NationData getOrCreateDefaultNationsInstance(MinecraftServer server) {
        return getOrCreateNationInstance(server.overworld());
    }

    public static NationData loadStatic(CompoundTag compoundTag,ServerLevel level) {
        NationData id = new NationData();
        id.load(compoundTag,level);
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

    public void renameNation(Nation nation, String name) {
        String oldName = nation.getName();
        nationsLookup.remove(oldName);
        nation.setName(name);
        nationsLookup.put(name,nation);
        setDirty();
    }

    public Nation getNationAtChunk(ChunkPos chunkPos) {
        return chunkLookup.get(chunkPos);
    }

    public void sendInvites(List<GameProfile> profiles, Nation nation, MinecraftServer server) {
        for (GameProfile gameProfile : profiles) {
            invites.put(gameProfile.getId(),nation);
            ServerPlayer player = server.getPlayerList().getPlayer(gameProfile.getId());
            if (player != null) {
                player.sendSystemMessage(Component.literal("You have been invited to nation "+nation.getName()));
            }
        }
        setDirty();
    }

    public void awardNearbyClaimsToAttackers(Nation attackers,Nation defenders,ChunkPos pos) {
        int radius = 1;
        for (int z = -radius; z < radius + 1;z++) {
            for (int x = -radius; x < radius + 1;x++) {
                ChunkPos around = new ChunkPos(pos.x+x,pos.z+z);
                Nation nation = chunkLookup.get(around);
                if (nation == defenders) {
                    removeClaim(defenders,around);
                    addClaim(attackers,around);
                }
            }
        }
    }

    public void sendAllyInvites(Nation fromNation,Nation toNation) {
        allyInvites.put(toNation,fromNation);
    }

    public void makeEnemy(MinecraftServer server,Nation fromNation,Nation toNation) {
        fromNation.getEnemies().add(toNation.getName());
        toNation.getEnemies().add(fromNation.getName());
        fromNation.getAllies().remove(toNation.getName());
        toNation.getAllies().remove(fromNation.getName());

        HashSet<GameProfile> allProfiles = new HashSet<>();
        allProfiles.addAll(toNation.getMembers());
        allProfiles.addAll(fromNation.getMembers());

        for (GameProfile gameProfile : allProfiles) {
            ServerPlayer player = server.getPlayerList().getPlayer(gameProfile.getId());
            if (player != null) {
                TeamHandler.updateOthers(player, this);
            }
        }

        setDirty();
    }

    public void makeNeutral(MinecraftServer server,Nation fromNation,Nation toNation) {
        fromNation.getEnemies().remove(toNation.getName());
        fromNation.getAllies().remove(toNation.getName());

        toNation.getEnemies().remove(fromNation.getName());
        toNation.getAllies().remove(fromNation.getName());

        HashSet<GameProfile> allProfiles = new HashSet<>();
        allProfiles.addAll(toNation.getMembers());
        allProfiles.addAll(fromNation.getMembers());

        for (GameProfile gameProfile : allProfiles) {
            ServerPlayer player = server.getPlayerList().getPlayer(gameProfile.getId());
            if (player != null) {
                TeamHandler.updateOthers(player, this);
            }
        }

        setDirty();
    }

    public void removeAllyInvite(Nation fromNation,Nation toNation) {
        allyInvites.remove(toNation);
    }

    public Nation getInviteForPlayer(ServerPlayer player) {
        return invites.get(player.getUUID());
    }

    public void removeInvite(ServerPlayer player) {
        invites.remove(player.getUUID());
        setDirty();
    }

    public boolean removeNation(MinecraftServer server, String name) {
        Nation toRemove = nationsLookup.get(name);
        if (toRemove == null) return false;

        for (ChunkPos chunkPos : toRemove.getClaimed()) {
            chunkLookup.remove(chunkPos);
        }

        boolean b = nations.remove(toRemove);
        nationsLookup.remove(name);

        for (Nation nation : nations) {
            nation.getAllies().remove(toRemove.getName());
            nation.getEnemies().remove(toRemove.getName());
        }

        if (activeSiege != null && activeSiege.isInvolved(toRemove)) {
            endSiege(Siege.Result.TERMINATED);
        }

        for (GameProfile profile : toRemove.getMembers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(profile.getId());
            if (player != null) {
                TeamHandler.updateOthers(player, this);
            }
        }

        setDirty();
        return b;
    }

    public boolean endSiege(Siege.Result result) {
        if (activeSiege != null) {
            activeSiege.end(result,this);
            activeSiege = null;
            setDirty();
            return true;
        }
        return false;
    }

    public void startSiege(Nation attacking, Nation defending,ServerLevel level,ChunkPos pos) {
        if (activeSiege == null) {
            activeSiege = new Siege(attacking,defending,level , pos);
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(attacking.getName()+" is starting a siege on "+defending.getName())
                    ,false);
            TeamHandler.sendMessageToTeam(level.getServer(),Component.literal("Warning, claim at ("+pos.x+","+pos.z+") is under attack!"),defending);
        }
    }
    public void tick(ServerLevel level) {
        if (activeSiege != null) {
            activeSiege.tick();
        }

        boolean didAnything = false;
        for (Nation nation : nations) {
            didAnything |= nation.tick(level.getServer());
        }
        if (didAnything) setDirty();
    }

    public List<Nation> getNations() {
        return nations;
    }

    public Nation getNationByName(String name) {
        return nationsLookup.get(name);
    }

    public @Nullable Siege getActiveSiege() {
        return activeSiege;
    }

    public void addClaims(Nation nation, Set<ChunkPos> poss) {
        for (ChunkPos pos : poss) {
            addClaim(nation,pos);
        }
    }

    public Nation addClaim(Nation nation,ChunkPos chunkPos) {
        Nation existing = chunkLookup.get(chunkPos);
        if (existing == null) {
            nation.getClaimed().add(chunkPos);
            chunkLookup.put(chunkPos, nation);
            setDirty();
        }
        return existing;
    }

    public void removeClaim(Nation nation,ChunkPos chunkPos) {
        nation.getClaimed().remove(chunkPos);
        chunkLookup.remove(chunkPos);
        setDirty();
    }

    public void load(CompoundTag tag,ServerLevel level) {
        nations.clear();
        nationsLookup.clear();
        chunkLookup.clear();
        playerLookup.clear();
        ListTag listTag = tag.getList(Nations.MOD_ID, Tag.TAG_COMPOUND);
        for (Tag tag1 : listTag) {
            CompoundTag compoundTag = (CompoundTag) tag1;
            Nation nation = Nation.loadStatic(compoundTag);
            nation.getClaimed().forEach(chunkPos -> chunkLookup.put(chunkPos,nation));
            nation.getMembers().forEach(gameProfile -> playerLookup.put(gameProfile,nation));
            nations.add(nation);
            nationsLookup.put(nation.getName(),nation);
        }
        if (tag.contains("active_siege")) {
            activeSiege = Siege.load(tag.getCompound("active_siege"), level,this);
        }
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        ListTag listTag = new ListTag();
        for (Nation nation : nations) {
            listTag.add(nation.save());
        }
        pCompoundTag.put(Nations.MOD_ID, listTag);
        if (activeSiege != null) {
            pCompoundTag.put("active_siege",activeSiege.save());
        }

        return pCompoundTag;
    }

    public void createAllianceBetween(MinecraftServer server,Nation fromNation,Nation toNation) {
        fromNation.getAllies().add(toNation.getName());
        toNation.getAllies().add(fromNation.getName());


        HashSet<GameProfile> allProfiles = new HashSet<>();
        allProfiles.addAll(toNation.getMembers());
        allProfiles.addAll(fromNation.getMembers());

        for (GameProfile gameProfile : allProfiles) {
            ServerPlayer player = server.getPlayerList().getPlayer(gameProfile.getId());
            if (player != null) {
                TeamHandler.updateOthers(player, this);
            }
        }


        setDirty();
    }

    public boolean joinNation(String name, Collection<ServerPlayer> serverPlayers) {
        Nation nation = getNationByName(name);
        if (nation != null) {
            nation.addPlayers(serverPlayers, this);
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

    public List<Nation> getNonAllianceNations(Nation nation) {
        List<String> otherNations = new ArrayList<>(nationsLookup.keySet());
        otherNations.remove(nation.getName());
        otherNations.removeAll(nation.getAllies());
        List<Nation> nations1 = otherNations.stream().map(nationsLookup::get).collect(Collectors.toList());
        return nations1;
    }

    @Nullable
    public Nation getAllianceInvite(Nation toNation) {
        return allyInvites.get(toNation);
    }

    public List<Nation> getNonEnemyNations(Nation nation) {
        List<String> otherNations = new ArrayList<>(nationsLookup.keySet());
        otherNations.remove(nation.getName());
        otherNations.removeAll(nation.getEnemies());
        List<Nation> nations1 = otherNations.stream().map(nationsLookup::get).collect(Collectors.toList());
        return nations1;
    }

    public List<Nation> getNonNeutralNations(Nation nation) {
        List<String> otherNations = new ArrayList<>(nationsLookup.keySet());
        otherNations.remove(nation.getName());

        for (String s : nationsLookup.keySet()) {
            if (!nation.getAllies().contains(s) && !nation.getEnemies().contains(s)) {
                otherNations.remove(s);
            }
        }

        List<Nation> nations1 = otherNations.stream().map(nationsLookup::get).collect(Collectors.toList());
        return nations1;
    }

    public void promote(GameProfile gameProfile,Nation nation) {
        nation.promote(gameProfile);
        setDirty();
    }

    public void demote(GameProfile gameProfile,Nation nation) {
        nation.demote(gameProfile);
        setDirty();
    }

    public boolean leaveNation(Collection<ServerPlayer> serverPlayers) {
        for (Nation nation : nations) {
            nation.removePlayers(serverPlayers, this);
        }
        setDirty();
        return true;
    }

    @Override
    public void setDirty() {
        super.setDirty();
        playerLookup.clear();
        nations.forEach(nation -> nation.getMembers().forEach(gameProfile -> playerLookup.put(gameProfile,nation)));
    }

    public boolean leaveNationGameProfiles(MinecraftServer server, Collection<GameProfile> serverPlayers) {
        for (Nation nation : nations) {
            nation.removeGameProfiles(server,serverPlayers,this);
        }

        setDirty();
        return true;
    }

    public Nation getNationOf(ServerPlayer player) {
        return playerLookup.get(player.getGameProfile());
    }

}
