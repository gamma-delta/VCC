package me.gammadelta.common.item;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import me.gammadelta.common.block.BlockMotherboard;
import me.gammadelta.common.block.VCCBlocks;

import static me.gammadelta.VCCMod.MOD_ID;

public class VCCItems {
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

	public static final ItemGroup VCC_ITEM_GROUP = new ItemGroup(MOD_ID) {
		@Override
		public ItemStack createIcon() {
			return new ItemStack(MOTHERBOARD_ITEM.get());
		}
	};

	public static final RegistryObject<Item> MOTHERBOARD_ITEM = ITEMS.register(BlockMotherboard.NAME,
			() -> new BlockItem(VCCBlocks.MOTHERBOARD_BLOCK.get(), new Item.Properties().group(VCC_ITEM_GROUP)));


	public static void register(){
		ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
	}
}
