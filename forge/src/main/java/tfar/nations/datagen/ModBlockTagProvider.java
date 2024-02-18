package tfar.nations.datagen;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import tfar.nations.ModTags;
import tfar.nations.Nations;

import java.util.ArrayList;
import java.util.List;

public class ModBlockTagProvider extends BlockTagsProvider {


    public ModBlockTagProvider(DataGenerator output, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Nations.MOD_ID, existingFileHelper);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addTags() {
        List<Block> entityBlocks = new ArrayList<>();
        for (Block block : Registry.BLOCK) {
            if (block instanceof EntityBlock) {
                entityBlocks.add(block);
            }
        }
        tag(ModTags.CLAIM_RESISTANT).addTags(BlockTags.DOORS,BlockTags.TRAPDOORS).add(entityBlocks.toArray(new Block[0]));
    }
}
