package me.gammadelta.datagen;

import me.gammadelta.common.block.BlockChassis;
import me.gammadelta.common.block.BlockMotherboard;
import me.gammadelta.common.block.VCCBlocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.data.DataGenerator;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.function.Function;

import static me.gammadelta.VCCMod.MOD_ID;

public class BlockStates extends BlockStateProvider {
    public BlockStates(DataGenerator gen, ExistingFileHelper exFileHelper) {
        super(gen, MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        singleTextureBlock(VCCBlocks.CHASSIS_BLOCK.get(), BlockChassis.NAME, "block/chassis");
        registerMotherboard();
    }

    public void singleTextureBlock(Block block, String modelName, String textureName) {
        ModelFile model = models().cubeAll(modelName, modLoc(textureName));
        simpleBlock(block, model);
    }

    private void registerMotherboard() {
        ResourceLocation side = new ResourceLocation(MOD_ID, "block/chassis");
        BlockModelBuilder unlit = models().cube(BlockMotherboard.NAME + "_unlit", side, side,
                new ResourceLocation(MOD_ID, "block/motherboard_front_unlit"), side, side, side);
        // by registering the default name here, we make its display in the inventory
        // be the lit version
        BlockModelBuilder lit = models().cube(BlockMotherboard.NAME, side, side,
                new ResourceLocation(MOD_ID, "block/motherboard_front_lit"), side, side, side);
        orientedBlock(VCCBlocks.MOTHERBOARD_BLOCK.get(), state -> {
            if (state.get(BlockStateProperties.LIT)) {
                return lit;
            } else {
                return unlit;
            }
        });
    }

    // free-range code yoinked directly from mcjty
    private void orientedBlock(Block block, Function<BlockState, ModelFile> modelFunc) {
        getVariantBuilder(block)
                .forAllStates(state -> {
                    Direction dir = state.get(BlockStateProperties.FACING);
                    return ConfiguredModel.builder()
                            .modelFile(modelFunc.apply(state))
                            .rotationX(dir.getAxis() == Direction.Axis.Y ?  dir.getAxisDirection().getOffset() * -90 : 0)
                            .rotationY(dir.getAxis() != Direction.Axis.Y ? ((dir.getHorizontalIndex() + 2) % 4) * 90 : 0)
                            .build();
                });
    }
}
