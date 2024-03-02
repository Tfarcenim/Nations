package tfar.nations;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
        MinecraftForge.EVENT_BUS.addListener(this::playerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(this::blockBreak);
        MinecraftForge.EVENT_BUS.addListener(this::blockInteract);
        MinecraftForge.EVENT_BUS.addListener(this::levelTick);
        MinecraftForge.EVENT_BUS.addListener(this::teleportEvent);
        MinecraftForge.EVENT_BUS.addListener(this::teleportPearlEvent);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW,this::playerDied);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Datagen::gather);
        Nations.init();
    }

    private void teleportEvent(EntityTeleportEvent.ChorusFruit event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer serverPlayer) {
            if (onTeleportForge(serverPlayer, event.getTarget())) {
                event.setCanceled(true);
            }
        }
    }

    private void teleportPearlEvent(EntityTeleportEvent.EnderPearl event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer serverPlayer) {
            if (onTeleportForge(serverPlayer, event.getTarget())) {
                event.setCanceled(true);
            }
        }
    }

    private boolean onTeleportForge(ServerPlayer player, Vec3 target) {
        NationData nationData = NationData.getOrCreateNationInstance(player.getLevel());
        Siege siege = nationData.getActiveSiege();
        if (siege != null) {
            if (siege.isAttacking(player, nationData) && siege.shouldBlockAttackers() && TeamHandler.isPointInArea(target, siege.getClaimPos(), 1)) {
                player.sendSystemMessage(Component.literal("Can't move into enemy claim during start of siege"));
                return true;
            }
        }
        return false;
    }

    private void levelTick(TickEvent.LevelTickEvent event) {
        if (event.level instanceof ServerLevel serverLevel && event.phase == TickEvent.Phase.START) {
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
                Nation nation = nationData.getNationOf(serverPlayer);
                if (nationChunk == nation || nationChunk.isAlly(nation)) {

                } else {
                    Siege siege = nationData.getActiveSiege();
                    if (siege != null && siege.getClaimPos().equals(chunkPos) && !siege.shouldBlockAttackers()) {
                        return;
                    }
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
                Nation nation = nationData.getNationOf(serverPlayer);
                if (nationChunk == nation || nationChunk.isAlly(nation)) {

                } else {
                    Siege siege = nationData.getActiveSiege();
                    if (siege != null && siege.getClaimPos().equals(chunkPos) && !siege.shouldBlockAttackers()) {
                        return;
                    }
                    event.setCanceled(true);
                }
            }
        }
    }

    private void playerDied(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            NationData data = NationData.getOrCreateDefaultNationsInstance(serverPlayer.server);
            Siege siege = data.getActiveSiege();
            if (siege != null) {
                siege.attackerDefeated(serverPlayer);
                siege.defenderDefeated(serverPlayer);
            }
        }
    }
    private void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Nations.logout((ServerPlayer) event.getEntity());
    }

    private void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Nations.login((ServerPlayer) event.getEntity());
    }

    private void onCommandRegister(RegisterCommandsEvent event) {
        Nations.onCommandRegister(event.getDispatcher());
    }
}