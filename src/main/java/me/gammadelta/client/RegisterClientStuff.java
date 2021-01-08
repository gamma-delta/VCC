package me.gammadelta.client;

import me.gammadelta.client.gui.GuiPuncher;
import me.gammadelta.common.block.VCCBlocks;
import me.gammadelta.common.item.VCCItems;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static me.gammadelta.VCCMod.MOD_ID;
import static me.gammadelta.common.item.ItemCoupon.*;

@Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegisterClientStuff {
    @SubscribeEvent
    public static void init(final FMLClientSetupEvent event) {
        ScreenManager.registerFactory(VCCBlocks.PUNCHER_CONTAINER.get(), GuiPuncher::new);

        event.enqueueWork(() -> ItemModelsProperties.registerProperty(VCCItems.COUPON.get(), COUPON_STATE_PREDICATE, (stack, world, holder) -> {
            CompoundNBT tag = stack.getOrCreateTag();
            if (tag.contains(COLLECTIBLE_INDEX_KEY)) {
                return 2f;
            } else if (tag.contains(ERROR_KEY)) {
                return 1f;
            } else {
                return 0f;
            }
        }));
    }
}
