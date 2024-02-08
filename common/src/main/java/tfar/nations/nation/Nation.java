package tfar.nations.nation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import tfar.nations.platform.Services;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Nation {

    private String name;
    private final Set<UUID> members = new HashSet<>();
    private int color =0xffffff;

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
            UUID uuid = player.getUUID();
            members.add(uuid);
            Services.PLATFORM.setNation(player,this);
        }
    }

    public void removePlayers(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            UUID uuid = player.getUUID();
            members.remove(uuid);
            Services.PLATFORM.setNation(player,null);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public CompoundTag save() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("name",name);
        ListTag listTag = new ListTag();
        for (UUID uuid : members) {
            listTag.add(StringTag.valueOf(uuid.toString()));
        }
        compoundTag.put("members",listTag);
        compoundTag.putInt("color",color);
        return compoundTag;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public static Nation loadStatic(CompoundTag tag) {
        Nation nation = new Nation();
        nation.name = tag.getString("name");
        ListTag listTag = tag.getList("members", Tag.TAG_STRING);
        for (Tag tag1 : listTag) {
            StringTag stringTag = (StringTag) tag1;
            nation.members.add(UUID.fromString(stringTag.getAsString()));
        }
        nation.color = tag.getInt("color");
        return nation;
    }
}
