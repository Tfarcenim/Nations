package tfar.nations;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static final TagKey<Block> CLAIM_RESISTANT = new TagKey<>(Registry.BLOCK_REGISTRY,new ResourceLocation(Nations.MOD_ID,"claim_resistant"));

}
