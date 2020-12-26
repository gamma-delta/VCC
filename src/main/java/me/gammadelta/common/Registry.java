package me.gammadelta.common;

import me.gammadelta.common.block.BlockMotherboard;
import me.gammadelta.common.block.VCCBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static me.gammadelta.VCCMod.MOD_ID;

public class Registry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(
            ForgeRegistries.TILE_ENTITIES, MOD_ID);

    public static final class Blocks {
        public static final RegistryObject<BlockMotherboard> MOTHERBOARD = BLOCKS.register(BlockMotherboard.NAME, BlockMotherboard::new);
    }

    public static final class Items {
        public static final RegistryObject<Item> MOTHERBOARD = ITEMS.register();

        private static final RegistryObject<Item> blockItem(Class<? extends VCCBlock> blockClass) {
            return ITEMS.register(blockClass., )
        }
    }

    public static void init() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        TILES.register(bus);
    }
}
