package me.gammadelta;

import me.gammadelta.client.ClientProxy;
import me.gammadelta.common.CommonProxy;
import me.gammadelta.common.block.VCCBlocks;
import me.gammadelta.common.item.VCCItems;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(VCCMod.MOD_ID)
public class VCCMod {
    public static final String MOD_ID = "vcc";

    private static VCCMod instance;
    private final CommonProxy proxy;
    private final SimpleChannel network;

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public VCCMod() {
        instance = this;

        // what tf does this do
        this.proxy = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
        this.network = NetworkRegistry.newSimpleChannel(new ResourceLocation(MOD_ID, "network"), () -> "1.0", s -> true,
                s -> true);
        this.proxy.init();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        VCCBlocks.register();
        VCCItems.register();
    }

    public static VCCMod getInstance() {
        return instance;
    }

    public static CommonProxy getProxy() {
        return getInstance().proxy;
    }

    public static SimpleChannel getNetwork() {
        return getInstance().network;
    }
}
