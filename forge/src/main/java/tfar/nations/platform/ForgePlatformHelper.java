package tfar.nations.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import tfar.nations.nation.Nation;
import tfar.nations.nation.NationData;
import tfar.nations.platform.services.IPlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.isProduction();
    }

    @Override
    public void setNation(ServerPlayer player, Nation nation) {
        CompoundTag tag = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (nation != null) {
            tag.putString("nation", nation.getName());
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG,tag);
        } else {
            tag.remove("nation");
        }
    }

    @Override
    @Nullable
    public Nation getNation(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!tag.contains("nation")) return null;
        String s = tag.getString("nation");
        Nation nation = NationData.getDefaultNationsInstance(player.server).getNationByName(s);
        return nation;
    }
}