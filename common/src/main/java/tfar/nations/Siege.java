package tfar.nations;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import tfar.nations.nation.Nation;
import tfar.nations.nation.NationData;
import tfar.nations.platform.Services;

import java.util.*;
import java.util.stream.Collectors;

public class Siege {



    private Nation attacking;
    private Nation defending;
    private Set<GameProfile> surviving_attackers;
    private Map<GameProfile,Integer> surviving_defenders;
    private final ServerLevel level;
    private final ChunkPos claimPos;
    long time;
    long stageTime;

    ServerBossEvent serverBossEvent = new ServerBossEvent(Component.literal("Siege"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public Siege(Nation attacking, Nation defending, ServerLevel level, ChunkPos claimPos) {
        this.attacking = attacking;
        this.defending = defending;
        this.level = level;
        this.claimPos = claimPos;

        List<ServerPlayer> attackers = attacking.getOnlineMembers(level.getServer());
        List<ServerPlayer> defenders = defending.getOnlineMembers(level.getServer());

        surviving_attackers = attackers.stream().map(Player::getGameProfile).collect(Collectors.toSet());

        surviving_defenders = new HashMap<>();

        defenders.forEach(player -> surviving_defenders.put(player.getGameProfile(), 0));

        serverBossEvent.setName(Component.literal("Siege: Stage 1"));

        for (ServerPlayer serverPlayer : attackers) {
            serverBossEvent.addPlayer(serverPlayer);
        }

        for (ServerPlayer serverPlayer : defenders) {
            serverBossEvent.addPlayer(serverPlayer);
        }
    }

    public enum Stage {
        ONE(20 * 60 * 1),//5 minutes
        TWO(20 * 60 * 1),//10 minutes
        THREE(20 * 60 * 1);//5 minutes
        private final long ticks;

        Stage(long ticks) {
            this.ticks = ticks;
        }

        public static Stage last() {
            return Stage.values()[Stage.values().length - 1];
        }
    }

    public int desertionTimer() {
        //120 seconds 1,2
        //15 seconds 3
        return stage == Stage.THREE ? 15 * 20: 120 * 20;
    }

    public void attackerDefeated(ServerPlayer player) {
        surviving_attackers.remove(player.getGameProfile());
        checkSiegeCompletion();
    }

    public void defenderDefeated(ServerPlayer player) {
        surviving_defenders.remove(player.getGameProfile());
        checkSiegeCompletion();
    }

    public void checkSiegeCompletion() {
        if (surviving_attackers.isEmpty()) {
            NationData.getNationInstance(level.getServer().overworld()).endSiege(Result.DEFEAT);
        }
        if (surviving_defenders.isEmpty()) {
            NationData.getNationInstance(level.getServer().overworld()).endSiege(Result.VICTORY);
        }
    }


    public ChunkPos getClaimPos() {
        return claimPos;
    }

    private Stage stage = Stage.ONE;
    public Stage getStage() {
        return stage;
    }

    public boolean isInvolved(Nation nation) {
        return nation == attacking || nation == defending;
    }

    public boolean isPlayerInvolved(ServerPlayer player) {
        Nation nation = Services.PLATFORM.getNation(player);
        return isInvolved(nation);
    }

    public void tick() {
        time++;
        stageTime++;
        serverBossEvent.setProgress((float) stageTime/stage.ticks);
        if (stageTime >= stage.ticks) {
            setNextStage();
        }
        for (GameProfile profile : surviving_defenders.keySet()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(profile.getId());
            if (player != null) {
                if (!TeamHandler.isPlayerNearClaim(player,claimPos)) {
                    surviving_defenders.put(profile,surviving_defenders.get(profile) + 1);
                    if (checkClaimDesertion(player)) {
                        deserted.add(player);
                    }
                } else {
                    surviving_defenders.put(profile,0);
                }
            }
        }
        deserted.forEach(this::defenderDefeated);
        deserted.clear();
    }

    private final Set<ServerPlayer> deserted = new HashSet<>();
    public boolean checkClaimDesertion(ServerPlayer player) {
        int ticksAbandoned = surviving_defenders.get(player.getGameProfile());
        int ticksLeft = desertionTimer() - ticksAbandoned;
        if (ticksLeft < 201) {
            if (ticksLeft <= 0) return true;
            if (ticksLeft % 20 == 0) {
                player.sendSystemMessage(Component.literal("Warning! Move back to claim within "+(ticksLeft / 20) +" seconds!"));
            }
            return false;
        }
        return false;
    }

    public void setNextStage() {
        if (stage != Stage.last()) {
            stage = Stage.values()[stage.ordinal() + 1];
            stageTime = 0;
            serverBossEvent.setProgress(0);
            serverBossEvent.setName(Component.literal("Siege: Stage "+(stage.ordinal() + 1)));

        } else {
            NationData.getNationInstance(level.getServer().overworld()).endSiege(Result.DEFEAT);
        }
    }

    public enum Result {
        TERMINATED,VICTORY,DEFEAT;//from perspective of the attackers
    }

    public void end(Result result, NationData nationData) {
        serverBossEvent.removeAllPlayers();
        switch (result) {
            case TERMINATED -> {
                level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("Siege has been terminated"),false);
            }
            case VICTORY -> {
                level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("Attackers are victorious"),false);
                nationData.awardNearbyClaimsToAttackers(attacking,defending,claimPos);
            }
            case DEFEAT -> {
                level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("Attackers have been defeated"),false);
            }
        }
    }


    public boolean isAttacking(ServerPlayer player) {
        return Services.PLATFORM.getNation(player) == attacking;
    }

    public boolean shouldBlockAttackers() {
        return getStage() == Stage.ONE;
    }

    public Siege(CompoundTag tag, ServerLevel level, NationData lookup) {
        this.level = level;
        attacking = lookup.getNationByName(tag.getString("attacking"));
        defending = lookup.getNationByName(tag.getString("defending"));
        claimPos = new ChunkPos(tag.getInt("x"),tag.getInt("z"));
        stage = Stage.values()[tag.getInt("stage")];
        time = tag.getLong("time");
        stageTime = tag.getLong("stage_time");
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("attacking",attacking.getName());
        tag.putString("defending",defending.getName());
        tag.putInt("x", claimPos.x);
        tag.putInt("z", claimPos.z);
        tag.putInt("stage",stage.ordinal());
        tag.putLong("time",time);
        tag.putLong("stage_time",stageTime);
        return tag;
    }

    public static Siege load(CompoundTag tag,ServerLevel level,NationData lookup) {
        return new Siege(tag,level,lookup);
    }


}
