package me.gammadelta.common;

import me.gammadelta.VCCMod;
import me.gammadelta.common.network.HighlightBlocksResponse;
import me.gammadelta.common.network.MotherboardOwnsWhatQuery;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.io.File;

import static me.gammadelta.VCCMod.MOD_ID;

/**
 * i have no idea what I'm doing
 * what is this and why does every mod have one
 */
public class CommonProxy {
    public CommonProxy() {

    }

    public void init() {
        SimpleChannel network = VCCMod.getNetwork();

        network.registerMessage(0, MotherboardOwnsWhatQuery.class, MotherboardOwnsWhatQuery::writeToBytes,
                MotherboardOwnsWhatQuery::new, MotherboardOwnsWhatQuery::handle);
        network.registerMessage(1, HighlightBlocksResponse.class, HighlightBlocksResponse::writeToBytes,
                HighlightBlocksResponse::new, HighlightBlocksResponse::handle);
    }

    /**
     * Return the directory VCC can write stuff to, specific to the world.
     * thanks, https://github.com/HellFirePvP/AstralSorcery/blob/e1b8c88ba71360c36232abfafc9b97406c63c398/src/main/java/hellfirepvp/astralsorcery/common/CommonProxy.java#L301
     */
    public File getVCCServerDataDirectory() {
        MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        if (server == null) {
            // we must be on the client?
            return null;
        }

        // ooh boy unmapped functions!
        // that's how you know you're doing something right
        File asDataDir = server.func_240776_a_(new FolderName(MOD_ID)).toFile();
        if (!asDataDir.exists()) {
            asDataDir.mkdirs();
        }
        return asDataDir;
    }
}
