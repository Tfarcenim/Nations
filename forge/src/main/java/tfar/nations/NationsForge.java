package tfar.nations;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(Nations.MOD_ID)
public class NationsForge {
    
    public NationsForge() {
    
        // This method is invoked by the Forge mod loader when it is ready
        // to load your mod. You can access Forge and Common code in this
        // project.
    
        // Use Forge to bootstrap the Common mod.
       // Nations.LOG.info("Hello Forge world!");

        MinecraftForge.EVENT_BUS.addListener(this::onCommandRegister);

        Nations.init();
    }


    private void onCommandRegister(RegisterCommandsEvent event) {
        Nations.onCommandRegister(event.getDispatcher());
    }
}