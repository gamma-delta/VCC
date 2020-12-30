package me.gammadelta.datagen;

import me.gammadelta.common.block.BlockChassis;
import me.gammadelta.common.block.BlockMotherboard;
import me.gammadelta.common.block.BlockRegister;
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
        registerRegister();
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

    // this isn't confusing
    private void registerRegister() {
        ResourceLocation sideUnlit = new ResourceLocation(MOD_ID, "block/register_side_unlit");
        ResourceLocation endUnlit = new ResourceLocation(MOD_ID, "block/register_end_unlit");
        BlockModelBuilder unlit = models().cubeColumn(BlockRegister.NAME + "_unlit", sideUnlit, endUnlit);

        ResourceLocation sideLit = new ResourceLocation(MOD_ID, "block/register_side_lit");
        ResourceLocation endLit = new ResourceLocation(MOD_ID, "block/register_end_lit");
        BlockModelBuilder lit = models().cubeColumn(BlockRegister.NAME, sideLit, endLit);

        orientedVerticalBlock(VCCBlocks.REGISTER_BLOCK.get(), state -> {
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

    private void orientedVerticalBlock(Block block, Function<BlockState, ModelFile> modelFunc) {
        getVariantBuilder(block)
                .forAllStates(state -> {
                    Direction.Axis axis = state.get(BlockStateProperties.AXIS);
                    return ConfiguredModel.builder()
                            .modelFile(modelFunc.apply(state))
                            .rotationX(axis != Direction.Axis.Y ? 90 : 0)
                            .rotationY(axis == Direction.Axis.X ? 90 : 0)
                            .build();
                });
    }
}
