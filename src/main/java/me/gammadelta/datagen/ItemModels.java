package me.gammadelta.datagen;

import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.RegistryObject;

import me.gammadelta.common.block.VCCBlocks;

import static me.gammadelta.VCCMod.MOD_ID;

public class ItemModels extends ItemModelProvider {

    public ItemModels(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        parentedBlock(VCCBlocks.CHASSIS_BLOCK.get(), "block/chassis");
        parentedBlock(VCCBlocks.MOTHERBOARD_BLOCK.get(), "block/motherboard");
        parentedBlock(VCCBlocks.REGISTER_BLOCK.get(), "block/register");
    }

    public void parentedBlock(Block block, String model) {
        getBuilder(block.getRegistryName().getPath())
                .parent(new ModelFile.UncheckedModelFile(modLoc(model)));
    }

    private void simpleItemBlock(RegistryObject<? extends Block> block) {
        this.singleTexture(block.get().getRegistryName().getPath(), new ResourceLocation("item/handheld"),
                "layer0", new ResourceLocation(MOD_ID, "block/" + block.get().getRegistryName().getPath()));
    }
}
