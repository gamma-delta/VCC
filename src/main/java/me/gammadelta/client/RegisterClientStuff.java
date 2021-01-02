package me.gammadelta.client;

import me.gammadelta.client.gui.GuiPuncher;
import me.gammadelta.common.block.VCCBlocks;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static me.gammadelta.VCCMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegisterClientStuff {
    @SubscribeEvent
    public static void init(final FMLClientSetupEvent event) {
        ScreenManager.registerFactory(VCCBlocks.PUNCHER_CONTAINER.get(), GuiPuncher::new);
    }
}
