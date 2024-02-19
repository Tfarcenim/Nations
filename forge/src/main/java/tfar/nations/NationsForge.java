package tfar.nations;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import tfar.nations.datagen.Datagen;
import tfar.nations.nation.Nation;
import tfar.nations.nation.NationData;
import tfar.nations.platform.Services;

@Mod(Nations.MOD_ID)
public class NationsForge {
    
    public NationsForge() {
    
        // This method is invoked by the Forge mod loader when it is ready
        // to load your mod. You can access Forge and Common code in this
        // project.
    
        // Use Forge to bootstrap the Common mod.
       // Nations.LOG.info("Hello Forge world!");

        MinecraftForge.EVENT_BUS.addListener(this::onCommandRegister);
        MinecraftForge.EVENT_BUS.addListener(this::playerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::blockBreak);
        MinecraftForge.EVENT_BUS.addListener(this::blockInteract);
        MinecraftForge.EVENT_BUS.addListener(this::levelTick);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Datagen::gather);
        Nations.init();
    }

    private void levelTick(TickEvent.LevelTickEvent event) {
        if (event.level instanceof ServerLevel serverLevel) {
            NationData nationData = NationData.getNationInstance(serverLevel);
            if (nationData != null) {
                nationData.tick(serverLevel);
            }
        }
    }

    private void blockInteract(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        BlockState state = player.level.getBlockState(pos);
        ChunkPos chunkPos = new ChunkPos(pos);
        if (player instanceof ServerPlayer serverPlayer) {
            NationData nationData = NationData.getOrCreateDefaultNationsInstance(serverPlayer.server);
            Nation nationChunk = nationData.getNationAtChunk(chunkPos);
            if (nationChunk == null) return;
            if (state.is(ModTags.CLAIM_RESISTANT)) {
                Nation nation = Services.PLATFORM.getNation(serverPlayer);
                if (nationChunk == nation || nationChunk.isAlly(nation)) {

                } else {
                    event.setCanceled(true);
                }
            }
        }
    }

    private void blockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        ChunkPos chunkPos = new ChunkPos(pos);
        if (player instanceof ServerPlayer serverPlayer) {
            NationData nationData = NationData.getOrCreateDefaultNationsInstance(serverPlayer.server);
            Nation nationChunk = nationData.getNationAtChunk(chunkPos);
            if (nationChunk == null) return;
            if (state.is(ModTags.CLAIM_RESISTANT)) {
                Nation nation = Services.PLATFORM.getNation(serverPlayer);
                if (nationChunk == nation || nationChunk.isAlly(nation)) {

                } else {
                    event.setCanceled(true);
                }
            }
        }
    }

    private void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Nations.login((ServerPlayer) event.getEntity());
    }

    private void onCommandRegister(RegisterCommandsEvent event) {
        Nations.onCommandRegister(event.getDispatcher());
    }
}