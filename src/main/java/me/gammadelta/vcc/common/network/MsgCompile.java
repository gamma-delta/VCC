package me.gammadelta.vcc.common.network;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.bytes.ByteList;
import me.gammadelta.vcc.VCCMod;
import me.gammadelta.vcc.common.block.tile.ContainerPuncher;
import me.gammadelta.vcc.common.item.ItemCoupon;
import me.gammadelta.vcc.common.item.VCCItems;
import me.gammadelta.vcc.common.program.compilation.ASMCompiler;
import me.gammadelta.vcc.common.program.compilation.CodeCompileException;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Sent client -> server when the player wants to compile something.
 */
public class MsgCompile {
    private final int windowID;
    private final String[] input;

    public MsgCompile(int windowID, String[] input) {
        this.windowID = windowID;
        this.input = input;
    }

    // [int windowid] [int stringCount] [string* strings]
    public MsgCompile(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);

        this.windowID = packetBuffer.readVarInt();
        int stringCount = packetBuffer.readVarInt();
        this.input = new String[stringCount];
        for (int i = 0; i < stringCount; i++) {
            this.input[i] = packetBuffer.readString();
        }
    }

    public void writeToBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);

        packetBuffer.writeVarInt(this.windowID);
        packetBuffer.writeVarInt(this.input.length);
        for (String page : this.input) {
            packetBuffer.writeString(page);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayerEntity sender = context.get().getSender();
            if (sender != null) {
                // yep, this is client -> server and we're the server
                if (sender.currentWindowId != this.windowID) {
                    // uh-oh
                    return;
                }
                ContainerPuncher container = (ContainerPuncher) sender.openContainer;
                if (container == null) {
                    // uh-oh redux
                    return;
                }

                // Compiling time!
                Slot paymentSlot = container.getSlot(1);
                ItemStack paymentStack = paymentSlot.getStack();
                if (paymentStack.getCount() >= 1) {
                    paymentSlot.decrStackSize(1);
                    // TODO: search for a villager
                    try {
                        ByteList compiled = ASMCompiler.compile(this.input);
                        byte[] compiledArr = compiled.toByteArray();
                        container.setMemory(compiledArr);
                        VCCMod.getNetwork()
                                .send(PacketDistributor.PLAYER.with(() -> sender),
                                        new MsgSyncMemoryInGui(compiledArr, windowID));
                    } catch (CodeCompileException cce) {
                        ItemStack problemCoupon = new ItemStack(VCCItems.COUPON.get());
                        CompoundNBT tag = problemCoupon.getOrCreateTag();
                        tag.put(ItemCoupon.ERROR_KEY, cce.serializeAll());
                        container.getSlot(2).putStack(problemCoupon);
                    }
                    container.markDirty();
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
