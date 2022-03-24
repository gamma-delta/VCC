package me.gammadelta.vcc.datagen;

import me.gammadelta.vcc.common.block.*;
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

import static me.gammadelta.vcc.VCCMod.MOD_ID;

public class BlockStates extends BlockStateProvider {
    public BlockStates(DataGenerator gen, ExistingFileHelper exFileHelper) {
        super(gen, MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        singleTextureComponent(VCCBlocks.CHASSIS_BLOCK.get(), "chassis");
        singleTextureComponent(VCCBlocks.OVERCLOCK_BLOCK.get(), "overclock");
        singleTextureComponent(VCCBlocks.XRAM_BLOCK.get(), "xram");
        singleTextureComponent(VCCBlocks.EXRAM_BLOCK.get(), "exram");
        singleTextureComponent(VCCBlocks.ROM_BLOCK.get(), "rom");
        singleTextureComponent(VCCBlocks.RAM_BLOCK.get(), "ram");

        registerMotherboard();
        registerRegister();
        registerCPU();
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
        ResourceLocation front_unlit = new ResourceLocation(MOD_ID, "block/motherboard_front_unlit");
        BlockModelBuilder unlit = models().cube(BlockMotherboard.NAME + "_unlit", side_unlit, side_unlit,
                front_unlit, side_unlit, side_unlit, side_unlit).texture("particle", front_unlit);
        // by registering the default name here, we make its display in the inventory
        // be the lit, unticking version
        ResourceLocation side_lit = new ResourceLocation(MOD_ID, "block/chassis_lit");
        ResourceLocation rl_lit_untick = new ResourceLocation(MOD_ID, "block/motherboard_front_lit_untick");
        BlockModelBuilder lit_untick = models().cube(BlockMotherboard.NAME, side_lit, side_lit, rl_lit_untick
                , side_lit, side_lit, side_lit).texture("particle", rl_lit_untick);
        ResourceLocation rl_lit_tick = new ResourceLocation(MOD_ID, "block/motherboard_front_lit_tick");
        BlockModelBuilder lit_tick = models().cube(BlockMotherboard.NAME + "_tick", side_lit, side_lit,
                rl_lit_tick, side_lit, side_lit, side_lit).texture("particle", rl_lit_tick);
        orientedBlock(VCCBlocks.MOTHERBOARD_BLOCK.get(), state -> {
            if (!state.get(BlockStateProperties.LIT)) {
                return unlit;
            } else {
                if (state.get(VCCBlockStates.TICKING)) {
                    return lit_tick;
                } else {
                    return lit_untick;
                }
            }
        });
    }

    // this isn't confusing
    private void registerRegister() {
        ResourceLocation sideUnlit = new ResourceLocation(MOD_ID, "block/register_side_unlit");
        ResourceLocation endUnlit = new ResourceLocation(MOD_ID, "block/register_end_unlit");
        BlockModelBuilder unlit = models().cubeColumn(BlockRegister.NAME, sideUnlit, endUnlit)
                .texture("particle", endUnlit);

        ResourceLocation sideLit = new ResourceLocation(MOD_ID, "block/register_side_lit");
        ResourceLocation endLit = new ResourceLocation(MOD_ID, "block/register_end_lit");
        BlockModelBuilder lit = models().cubeColumn(BlockRegister.NAME + "_lit", sideLit, endLit)
                .texture("particle", endLit);

        orientedVerticalBlock(VCCBlocks.REGISTER_BLOCK.get(), state -> {
            if (state.get(BlockStateProperties.LIT)) {
                return lit;
            } else {
                return unlit;
            }
        });
    }

    private void registerCPU() {
        ResourceLocation unlit = new ResourceLocation(MOD_ID, "block/cpu_unlit");
        ResourceLocation unlitIP = new ResourceLocation(MOD_ID, "block/cpu_ip_unlit");
        ResourceLocation unlitSP = new ResourceLocation(MOD_ID, "block/cpu_sp_unlit");
        ResourceLocation litIP = new ResourceLocation(MOD_ID, "block/cpu_ip_lit");
        ResourceLocation litSP = new ResourceLocation(MOD_ID, "block/cpu_sp_lit");
        ResourceLocation litUntick = new ResourceLocation(MOD_ID, "block/cpu_untick");
        ResourceLocation litTick = new ResourceLocation(MOD_ID, "block/cpu_tick");

        BlockModelBuilder modelUnlit = models().cube(BlockCPU.NAME + "_unlit", unlit, unlit, unlitIP, unlitSP, unlit,
                unlit).texture("particle", unlit);
        BlockModelBuilder modelUntick = models().cube(BlockCPU.NAME, litUntick, litUntick, litIP, litSP, litUntick,
                litUntick).texture("particle", litUntick);
        BlockModelBuilder modelTick = models().cube(BlockCPU.NAME + "_tick", litTick, litTick, litIP, litSP, litTick,
                litTick).texture("particle", litTick);

        orientedBlock(VCCBlocks.CPU_BLOCK.get(), state -> {
            if (!state.get(BlockStateProperties.LIT)) {
                return modelUnlit;
            } else {
                if (state.get(VCCBlockStates.TICKING)) {
                    return modelTick;
                } else {
                    return modelUntick;
                }
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
