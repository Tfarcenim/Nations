package tfar.nations;

import net.minecraftforge.fml.common.Mod;

@Mod(Nations.MOD_ID)
public class NationsForge {
    
    public NationsForge() {
    
        // This method is invoked by the Forge mod loader when it is ready
        // to load your mod. You can access Forge and Common code in this
        // project.
    
        // Use Forge to bootstrap the Common mod.
        Nations.LOG.info("Hello Forge world!");
        Nations.init();
        
    }
}