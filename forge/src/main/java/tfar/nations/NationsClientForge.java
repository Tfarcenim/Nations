package tfar.nations;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import tfar.nations.client.NationsClient;

public class NationsClientForge {

    public static void clientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(NationsClientForge::registerClientCommands);
    }

    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("glow")
                .executes(NationsClientForge::toggleGlow));
    }

    private static int toggleGlow(CommandContext<CommandSourceStack> commandContext) {
        NationsClient.toggleGlow();
        return 1;
    }

}
