package tfar.nations;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import tfar.nations.nation.Nation;
import tfar.nations.nation.NationData;
import tfar.nations.platform.Services;

public class Siege {

    public static final int PRE_LENGTH = 60 * 20 * 1;

    private Nation attacking;
    private Nation defending;
    private final ServerLevel level;
    private final ChunkPos claimPos;
    long age;

    public Siege(Nation attacking, Nation defending, ServerLevel level, ChunkPos claimPos) {
        this.attacking = attacking;
        this.defending = defending;
        this.level = level;
        this.claimPos = claimPos;
    }

    public enum Stage {
        PRE,ONGOING;
    }

    public ChunkPos getClaimPos() {
        return claimPos;
    }

    public Stage getStage() {
        if (age < PRE_LENGTH) {
            return Stage.PRE;
        }
        return Stage.ONGOING;
    }

    public boolean isInvolved(Nation nation) {
        return nation == attacking || nation == defending;
    }

    public boolean isAttacking(ServerPlayer player) {
        return Services.PLATFORM.getNation(player) == attacking;
    }

    public Siege(CompoundTag tag, ServerLevel level, NationData lookup) {
        this.level = level;
        attacking = lookup.getNationByName(tag.getString("attacking"));
        defending = lookup.getNationByName(tag.getString("defending"));
        claimPos = new ChunkPos(tag.getInt("x"),tag.getInt("z"));

    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("attacking",attacking.getName());
        tag.putString("defending",defending.getName());
        tag.putInt("x", claimPos.x);
        tag.putInt("z", claimPos.z);
        return tag;
    }

    public static Siege load(CompoundTag tag,ServerLevel level,NationData lookup) {
        return new Siege(tag,level,lookup);
    }

    public void tick() {
        age++;
    }

}
