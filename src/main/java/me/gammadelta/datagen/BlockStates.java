package me.gammadelta.datagen;

import me.gammadelta.common.block.BlockMotherboard;
import me.gammadelta.common.block.BlockPuncher;
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
        singleTextureComponent(VCCBlocks.CHASSIS_BLOCK.get(), "chassis");
        singleTextureComponent(VCCBlocks.OVERCLOCK_BLOCK.get(), "overclock");
        registerMotherboard();
        registerRegister();
        registerPuncher();
    }

    public void singleTextureComponent(Block block, String modelName) {
        String textureName = "block/" + modelName;
        ModelFile lit = models().cubeAll(modelName + "_lit", modLoc(textureName + "_lit"));
        ModelFile unlit = models().cubeAll(modelName, modLoc(textureName + "_unlit"));
        getVariantBuilder(block).forAllStates(state -> {
            ModelFile theModel;
            if (state.get(BlockStateProperties.LIT)) {
                theModel = lit;
            } else {
                theModel = unlit;
            }
            return ConfiguredModel.builder().modelFile(theModel).build();
        });
    }

    private void registerMotherboard() {
        ResourceLocation side_unlit = new ResourceLocation(MOD_ID, "block/chassis_unlit");
        BlockModelBuilder unlit = models().cube(BlockMotherboard.NAME + "_unlit", side_unlit, side_unlit,
                new ResourceLocation(MOD_ID, "block/motherboard_front_unlit"), side_unlit, side_unlit, side_unlit);
        // by registering the default name here, we make its display in the inventory
        // be the lit version
        ResourceLocation side_lit = new ResourceLocation(MOD_ID, "block/chassis_lit");
        BlockModelBuilder lit = models().cube(BlockMotherboard.NAME, side_lit, side_lit,
                new ResourceLocation(MOD_ID, "block/motherboard_front_lit"), side_lit, side_lit, side_lit);
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
        BlockModelBuilder unlit = models().cubeColumn(BlockRegister.NAME, sideUnlit, endUnlit);

        ResourceLocation sideLit = new ResourceLocation(MOD_ID, "block/register_side_lit");
        ResourceLocation endLit = new ResourceLocation(MOD_ID, "block/register_end_lit");
        BlockModelBuilder lit = models().cubeColumn(BlockRegister.NAME + "_lit", sideLit, endLit);

        orientedVerticalBlock(VCCBlocks.REGISTER_BLOCK.get(), state -> {
            if (state.get(BlockStateProperties.LIT)) {
                return lit;
            } else {
                return unlit;
            }
        });
    }


    private void registerPuncher() {
        ResourceLocation top = new ResourceLocation(MOD_ID, "block/puncher_top");
        ResourceLocation side = new ResourceLocation(MOD_ID, "block/puncher_side");
        ResourceLocation bottom = new ResourceLocation(MOD_ID, "block/puncher_bottom");
        simpleBlock(VCCBlocks.PUNCHER_BLOCK.get(), models().cubeBottomTop(BlockPuncher.NAME, side, bottom, top));
    }

    // free-range code yoinked directly from mcjty
    private void orientedBlock(Block block, Function<BlockState, ModelFile> modelFunc) {
        getVariantBuilder(block)
                .forAllStates(state -> {
                    Direction dir = state.get(BlockStateProperties.FACING);
                    return ConfiguredModel.builder()
                            .modelFile(modelFunc.apply(state))
                            .rotationX(dir.getAxis() == Direction.Axis.Y ? dir.getAxisDirection().getOffset() * -90 : 0)
                            .rotationY(
                                    dir.getAxis() != Direction.Axis.Y ? ((dir.getHorizontalIndex() + 2) % 4) * 90 : 0)
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
