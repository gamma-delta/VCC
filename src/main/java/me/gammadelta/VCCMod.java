package me.gammadelta;

import me.gammadelta.client.VCCParticles;
import me.gammadelta.common.block.VCCBlocks;
import me.gammadelta.common.item.VCCItems;
import me.gammadelta.common.network.MsgHighlightBlocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(VCCMod.MOD_ID)
public class VCCMod {
    public static final String MOD_ID = "vcc";

    private static VCCMod instance;
    private final SimpleChannel network;

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public VCCMod() {
        instance = this;

        this.network = NetworkRegistry.newSimpleChannel(new ResourceLocation(MOD_ID, "network"), () -> "1.0", s -> true,
                s -> true);

        registerMessages();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        VCCBlocks.register();
        VCCItems.register();
        VCCParticles.register();
    }

    private void registerMessages() {
        int messageIdx = 0;
        network.registerMessage(messageIdx++, MsgHighlightBlocks.class, MsgHighlightBlocks::writeToBytes,
                MsgHighlightBlocks::new, MsgHighlightBlocks::handle);
    }

    public static VCCMod getInstance() {
        return instance;
    }

    public static SimpleChannel getNetwork() {
        return getInstance().network;
    }
}
