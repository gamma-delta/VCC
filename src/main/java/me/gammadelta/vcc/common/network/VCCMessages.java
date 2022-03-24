package me.gammadelta.vcc.common.network;

import net.minecraftforge.fml.network.simple.SimpleChannel;

public class VCCMessages {
    public static void register(SimpleChannel network) {
        int messageIdx = 0;
        network.registerMessage(messageIdx++, MsgHighlightBlocks.class, MsgHighlightBlocks::writeToBytes,
                MsgHighlightBlocks::new, MsgHighlightBlocks::handle);
        network.registerMessage(messageIdx++, MsgSyncMemoryInGui.class, MsgSyncMemoryInGui::writeToBytes,
                MsgSyncMemoryInGui::new, MsgSyncMemoryInGui::handle);
        network.registerMessage(messageIdx++, MsgRequestMemoryInGui.class, MsgRequestMemoryInGui::writeToBytes,
                MsgRequestMemoryInGui::new, MsgRequestMemoryInGui::handle);
        network.registerMessage(messageIdx++, MsgCompile.class, MsgCompile::writeToBytes,
                MsgCompile::new, MsgCompile::handle);
        network.registerMessage(messageIdx++, MsgPunch.class, MsgPunch::writeToBytes,
                MsgPunch::new, MsgPunch::handle);
    }
}
