package me.gammadelta.datagen;

import me.gammadelta.common.item.VCCItems;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import me.gammadelta.common.block.VCCBlocks;

import static me.gammadelta.VCCMod.MOD_ID;

public class ItemModels extends ItemModelProvider {

    public ItemModels(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simpleItem(VCCItems.CLIPBOARD.get());
        simpleItem(VCCItems.PUNCHCARD.get());
        simpleItem(VCCItems.FILLED_PUNCHCARD.get());

        parentedBlock(VCCBlocks.MOTHERBOARD_BLOCK.get(), "block/motherboard");
        parentedBlock(VCCBlocks.CHASSIS_BLOCK.get(), "block/chassis");
        parentedBlock(VCCBlocks.REGISTER_BLOCK.get(), "block/register");
        parentedBlock(VCCBlocks.OVERCLOCK_BLOCK.get(), "block/overclock");

        parentedBlock(VCCBlocks.PUNCHER_BLOCK.get(), "block/overclock");
    }

    public void parentedBlock(Block block, String model) {
        getBuilder(block.getRegistryName().getPath())
                .parent(new ModelFile.UncheckedModelFile(modLoc(model)));
    }

    public void simpleItem(Item item) {
        singleTexture(item.getRegistryName().getPath(), new ResourceLocation("item/handheld"),
                "layer0", new ResourceLocation(MOD_ID, "item/" + item.getRegistryName().getPath()));
    }
}
