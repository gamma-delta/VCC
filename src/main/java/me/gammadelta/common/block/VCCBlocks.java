package me.gammadelta.common.block;

import me.gammadelta.common.block.tile.ContainerPuncher;
import me.gammadelta.common.block.tile.TileMotherboard;
import me.gammadelta.common.block.tile.TilePuncher;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static me.gammadelta.VCCMod.MOD_ID;

public class VCCBlocks {
    private static AbstractBlock.Properties COMPONENT_PROPS = AbstractBlock.Properties.create(Material.IRON)
            .hardnessAndResistance(1.5f)
            .harvestTool(
                    ToolType.PICKAXE);

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(
            ForgeRegistries.TILE_ENTITIES, MOD_ID);
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(
            ForgeRegistries.CONTAINERS, MOD_ID);

    // region Register blocks

    public static final RegistryObject<Block> MOTHERBOARD_BLOCK = BLOCKS.register(BlockMotherboard.NAME,
            BlockMotherboard::new);
    public static final RegistryObject<Block> CPU_BLOCK = BLOCKS.register(BlockCPU.NAME,
            BlockCPU::new);
    // a register block, not register a block
    public static final RegistryObject<Block> REGISTER_BLOCK = BLOCKS.register(BlockRegister.NAME,
            BlockRegister::new);
    public static final RegistryObject<Block> CHASSIS_BLOCK = component("chassis");
    public static final RegistryObject<Block> OVERCLOCK_BLOCK = component("overclock");
    public static final RegistryObject<Block> PUNCHER_BLOCK = BLOCKS.register(BlockPuncher.NAME,
            BlockPuncher::new);

    // endregion

    // region Register tile entities

    public static final RegistryObject<TileEntityType<TileMotherboard>> MOTHERBOARD_TILE = TILES.register(
            BlockMotherboard.NAME,
            () -> TileEntityType.Builder.create(TileMotherboard::new, MOTHERBOARD_BLOCK.get()).build(null));
    public static final RegistryObject<TileEntityType<TilePuncher>> PUNCHER_TILE = TILES.register(
            BlockPuncher.NAME,
            () -> TileEntityType.Builder.create(TilePuncher::new, PUNCHER_BLOCK.get()).build(null));

    // endregion

    // region Register containers

    public static final RegistryObject<ContainerType<ContainerPuncher>> PUNCHER_CONTAINER = CONTAINERS.register(
            BlockPuncher.NAME,
            () -> IForgeContainerType.create(((windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                World world = inv.player.getEntityWorld();
                return new ContainerPuncher(windowId, world, pos, inv, inv.player);
            }))
    );

    // endregion

    private static RegistryObject<Block> simpleBlock(String name, AbstractBlock.Properties props) {
        return BLOCKS.register(name, () -> new Block(props));
    }

    private static RegistryObject<Block> component(String name) {
        return BLOCKS.register(name, BlockComponent::new);
    }

    public static void register() {
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        TILES.register(FMLJavaModLoadingContext.get().getModEventBus());
        CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
