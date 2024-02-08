package tfar.nations;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class Nations {

    public static final String MOD_ID = "nations";
    public static final String MOD_NAME = "Nations";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    // The loader specific projects are able to import and use any code from the common project. This allows you to
    // write the majority of your code here and load it from your loader specific projects. This example has some
    // code that gets invoked by the entry point of the loader specific projects.
    public static void init() {

     //   LOG.info("Hello from Common init on {}! we are currently in a {} environment!", Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
     //   LOG.info("The ID for diamonds is {}", BuiltInRegistries.ITEM.getKey(Items.DIAMOND));

        // It is common for all supported loaders to provide a similar feature that can not be used directly in the
        // common code. A popular way to get around this is using Java's built-in service loader feature to create
        // your own abstraction layer. You can learn more about this in our provided services class. In this example
        // we have an interface in the common code and use a loader specific implementation to delegate our call to
        // the platform specific approach.
    //    if (Services.PLATFORM.isModLoaded("examplemod")) {

    //        LOG.info("Hello to examplemod");
    //    }
    }

    public static void onCommandRegister(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(MOD_ID)
                .executes(Nations::openGui));
    }

    private static int openGui(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer player = commandContext.getSource().getPlayerOrException();
        player.openMenu(new SimpleMenuProvider((i, inventory, player1) -> new CreateNationsMenu(i,inventory, createNationContainer(player1)),
                Component.literal("Create Nation")));
        return 1;
    }

    private static Container createNationContainer(Player player1) {
        SimpleContainer simpleContainer = new SimpleContainer(9) {
            @Override
            public ItemStack removeItem(int slot, int amount) {
                return super.removeItem(slot, amount);
            }
        };
        ItemStack yes = new ItemStack(Items.GREEN_WOOL);
        yes.setHoverName(Component.literal("Yes"));
        simpleContainer.setItem(0,yes);

        ItemStack no = new ItemStack(Items.RED_WOOL);
        no.setHoverName(Component.literal("No"));
        simpleContainer.setItem(8,no);

        return simpleContainer;
    }

}