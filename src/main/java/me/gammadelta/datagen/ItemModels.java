package me.gammadelta.datagen;

import me.gammadelta.VCCRegistry;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.RegistryObject;

import static me.gammadelta.VCCMod.MOD_ID;

public class ItemModels extends ItemModelProvider {

    public ItemModels(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simpleItemBlock(VCCRegistry.Blocks.MOTHERBOARD);
        simpleItemBlock(VCCRegistry.Blocks.CHASSIS);
    }

    private void simpleItemBlock(RegistryObject<? extends Block> block) {
        this.singleTexture(block.get().getRegistryName().getPath(), new ResourceLocation("item/handheld"),
                "layer0", new ResourceLocation(MOD_ID, "item/" + block.get().getRegistryName()));
    }
}
