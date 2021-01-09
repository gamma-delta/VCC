package me.gammadelta.datagen;

import me.gammadelta.common.block.VCCBlocks;
import me.gammadelta.common.item.ItemCoupon;
import me.gammadelta.common.item.VCCItems;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

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

        simpleItem(modLoc("coupon_broken"));
        simpleItem(modLoc("coupon_error"));
        simpleItem(modLoc("coupon_collectible"));
        this.getBuilder(VCCItems.COUPON.get().getRegistryName().getPath())
                .override()
                .predicate(ItemCoupon.COUPON_STATE_PREDICATE, -0.01f)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/coupon_broken")))
                .end()
                .override()
                .predicate(ItemCoupon.COUPON_STATE_PREDICATE, 1f - 0.01f)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/coupon_error")))
                .end()
                .override()
                .predicate(ItemCoupon.COUPON_STATE_PREDICATE, 2f - 0.01f)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/coupon_collectible")))
                .end();

        parentedBlock(VCCBlocks.MOTHERBOARD_BLOCK.get(), "block/motherboard");
        parentedBlock(VCCBlocks.CHASSIS_BLOCK.get(), "block/chassis");
        parentedBlock(VCCBlocks.REGISTER_BLOCK.get(), "block/register");
        parentedBlock(VCCBlocks.CPU_BLOCK.get(), "block/cpu");
        parentedBlock(VCCBlocks.XRAM_BLOCK.get(), "block/xram");
        parentedBlock(VCCBlocks.EXRAM_BLOCK.get(), "block/exram");
        parentedBlock(VCCBlocks.ROM_BLOCK.get(), "block/rom");
        parentedBlock(VCCBlocks.RAM_BLOCK.get(), "block/ram");
        parentedBlock(VCCBlocks.OVERCLOCK_BLOCK.get(), "block/overclock");
        parentedBlock(VCCBlocks.PUNCHER_BLOCK.get(), "block/puncher");
    }

    public void parentedBlock(Block block, String model) {
        getBuilder(block.getRegistryName().getPath())
                .parent(new ModelFile.UncheckedModelFile(modLoc(model)));
    }

    public void simpleItem(Item item) {
        simpleItem(item.getRegistryName());
    }

    public void simpleItem(ResourceLocation path) {
        singleTexture(path.getPath(), new ResourceLocation("item/handheld"),
                "layer0", new ResourceLocation(MOD_ID, "item/" + path.getPath()));
    }
}
