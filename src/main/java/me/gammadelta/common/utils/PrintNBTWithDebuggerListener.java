package me.gammadelta.common.utils;

import me.gammadelta.common.block.tile.TileDumbComputerComponent;
import me.gammadelta.common.block.tile.TileMotherboard;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static me.gammadelta.VCCMod.MOD_ID;

/**
 * This listener makes VCC blocks print their NBT when hit with a sufficiently
 * high debug level (>= 5).
 */
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PrintNBTWithDebuggerListener {
    @SubscribeEvent
    public static void maybePrintNBT(PlayerInteractEvent.RightClickBlock e) {
        PlayerEntity player = e.getPlayer();
        World world = player.getEntityWorld();
        if (world.isRemote) {
            // nbt is only on the server so stop if we're on the client
            return;
        }

        BlockPos activationPos = e.getPos();

        int level = Utils.funniDebugLevel(player, e.getHand());
        if (level < 5) {
            return;
        }

        TileEntity te = world.getTileEntity(activationPos);
        if (te instanceof TileDumbComputerComponent || te instanceof TileMotherboard) {
            // poggers
            // i don't know what the write is for, but the `/data` command does it like that
            // using `getTileData`, as one would *EXPECT*, always returns an empty tag.
            // s m h
            CompoundNBT data = te.write(new CompoundNBT());
            ITextComponent pretty;
            if (e.getHand() == Hand.MAIN_HAND) {
                pretty = data.toFormattedComponent(" ", 0);
            } else {
                // use the offhand to get no indentation
                pretty = data.toFormattedComponent();
            }
            String name = te.getClass().getSimpleName();

            if (player.isSneaking()) {
                Minecraft.getInstance().keyboardListener.setClipboardString(pretty.getString());
                player.sendMessage(
                        new TranslationTextComponent("misc.debugNBT.clipboard", name, pretty.getString().length()),
                        Util.DUMMY_UUID);
            } else {
                player.sendMessage(new TranslationTextComponent("misc.debugNBT", name, pretty), Util.DUMMY_UUID);
            }
            e.setUseBlock(Event.Result.ALLOW);
        }
    }
}
