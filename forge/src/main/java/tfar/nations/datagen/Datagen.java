package tfar.nations.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

public class Datagen {

    public static void gather(GatherDataEvent event) {
        boolean client = event.includeClient();
        DataGenerator dataGenerator = event.getGenerator();
        //PackOutput packOutput = dataGenerator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();
        //CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        ModBlockTagProvider blockTagProvider = new ModBlockTagProvider(dataGenerator,helper);
        dataGenerator.addProvider(true,blockTagProvider);
    }
}
