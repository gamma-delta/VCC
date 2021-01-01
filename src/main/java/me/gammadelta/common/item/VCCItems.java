package me.gammadelta.common.item;

import me.gammadelta.common.block.BlockMotherboard;
import me.gammadelta.common.block.BlockPuncher;
import me.gammadelta.common.block.BlockRegister;
import me.gammadelta.common.block.VCCBlocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static me.gammadelta.VCCMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class VCCItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final ItemGroup VCC_ITEM_GROUP = new ItemGroup(MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(MOTHERBOARD_ITEM.get());
        }
    };

    // region Normal items

    public static final RegistryObject<Item> PUNCHCARD = dumbItem("punchcard");
    public static final RegistryObject<ItemFilledPunchCard> FILLED_PUNCHCARD = ITEMS.register(ItemFilledPunchCard.NAME,
            ItemFilledPunchCard::new);
    public static final RegistryObject<Item> CLIPBOARD = ITEMS.register(ItemClipboard.NAME, ItemClipboard::new);
    public static final RegistryObject<Item> COUPON = ITEMS.register(ItemCoupon.NAME, ItemCoupon::new);

    // endregion

    // region Block items

    public static final RegistryObject<Item> MOTHERBOARD_ITEM = ITEMS.register(BlockMotherboard.NAME,
            () -> new BlockItem(VCCBlocks.MOTHERBOARD_BLOCK.get(), new Item.Properties().group(VCC_ITEM_GROUP)));
    public static final RegistryObject<Item> CHASSIS_ITEM = ITEMS.register("chassis",
            () -> new BlockItem(VCCBlocks.CHASSIS_BLOCK.get(), new Item.Properties().group(VCC_ITEM_GROUP)));
    public static final RegistryObject<Item> REGISTER_ITEM = ITEMS.register(BlockRegister.NAME,
            () -> new BlockItem(VCCBlocks.REGISTER_BLOCK.get(), new Item.Properties().group(VCC_ITEM_GROUP)));
    public static final RegistryObject<Item> OVERCLOCK_ITEM = ITEMS.register("overclock",
            () -> new BlockItem(VCCBlocks.OVERCLOCK_BLOCK.get(), new Item.Properties().group(VCC_ITEM_GROUP)));
    public static final RegistryObject<Item> PUNCHER_ITEM = ITEMS.register(BlockPuncher.NAME,
            () -> new BlockItem(VCCBlocks.PUNCHER_BLOCK.get(), new Item.Properties().group(VCC_ITEM_GROUP)));

    // endregion

    private static RegistryObject<Item> dumbItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().group(VCC_ITEM_GROUP)));
    }


    public static void register() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
