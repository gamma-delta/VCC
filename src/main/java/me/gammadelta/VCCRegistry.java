package me.gammadelta;

import me.gammadelta.common.block.BlockChassis;
import me.gammadelta.common.block.BlockMotherboard;
import me.gammadelta.common.block.tile.TileChassis;
import me.gammadelta.common.block.tile.TileMotherboard;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static me.gammadelta.VCCMod.MOD_ID;

public class VCCRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(
            ForgeRegistries.TILE_ENTITIES, MOD_ID);

    public static final class Blocks {
        public static final RegistryObject<BlockMotherboard> MOTHERBOARD = BLOCKS.register(BlockMotherboard.NAME,
                BlockMotherboard::new);
        public static final RegistryObject<BlockChassis> CHASSIS = BLOCKS.register(BlockChassis.NAME,
                BlockChassis::new);
    }

    public static final class Tiles {
        public static final RegistryObject<TileEntityType<TileMotherboard>> MOTHERBOARD = TILES.register(
                BlockMotherboard.NAME,
                () -> TileEntityType.Builder.create(TileMotherboard::new, Blocks.MOTHERBOARD.get()).build(null));
        public static final RegistryObject<TileEntityType<TileChassis>> CHASSIS = TILES.register(
                BlockChassis.NAME,
                () -> TileEntityType.Builder.create(TileChassis::new, Blocks.CHASSIS.get()).build(null));
    }

    public static final class Items {
        public static final RegistryObject<Item> MOTHERBOARD = ITEMS.register(BlockMotherboard.NAME,
                () -> new BlockItem(Blocks.MOTHERBOARD.get(), new Item.Properties().group(VCC_ITEM_GROUP)));
    }

    public static final ItemGroup VCC_ITEM_GROUP = new ItemGroup(MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(Items.MOTHERBOARD.get());
        }
    };

    public static void init() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        TILES.register(bus);
    }
}
