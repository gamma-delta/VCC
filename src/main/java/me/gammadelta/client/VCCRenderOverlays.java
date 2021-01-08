package me.gammadelta.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.gammadelta.common.block.tile.TileMotherboard;
import me.gammadelta.common.item.ItemDebugoggles;
import me.gammadelta.common.item.VCCItems;
import me.gammadelta.common.program.CPURepr;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class VCCRenderOverlays {
    @SubscribeEvent
    public static void renderOverlay(RenderGameOverlayEvent e) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        // 3 = head slot
        ItemStack headArmor = player.inventory.armorInventory.get(3);
        if (headArmor.getItem() == VCCItems.DEBUGOGGLES.get()) {
            tryRenderComputerInfo(e, player, headArmor);
        }
    }

    private static void tryRenderComputerInfo(RenderGameOverlayEvent e, ClientPlayerEntity player,
            ItemStack debugoggles) {
        if (debugoggles.getOrCreateTag().contains(ItemDebugoggles.MOTHERBOARD_POS_KEY)) {
            // Render our info!
            BlockPos motherboardPos = NBTUtil.readBlockPos(
                    debugoggles.getTag().getCompound(ItemDebugoggles.MOTHERBOARD_POS_KEY));
            MatrixStack neo = e.getMatrixStack();
            TileMotherboard motherboard = (TileMotherboard) player.world.getTileEntity(motherboardPos);
            if (motherboard != null) {
                Quaternion cameraQuat = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getRotation();

                ArrayList<ArrayList<CPURepr>> cpus = motherboard.getMotherboard().cpus;
                for (int i = 0; i < cpus.size(); i++) {
                    ArrayList<CPURepr> cpuGroup = cpus.get(i);
                    for (CPURepr cpu : cpuGroup) {
                        renderNameplate(
                                neo, new Vector3f(cpu.manifestation.getX(), cpu.manifestation.getY(),
                                        cpu.manifestation.getZ()),
                                cameraQuat, new TranslationTextComponent("misc.debugoggles.cpu.idx", i),
                                Minecraft.getInstance().getRenderTypeBuffers().getBufferSource(), 0xF000F0);
                    }
                }
            }
        }
    }

    private static void renderNameplate(MatrixStack neo, Vector3f pos, Quaternion camRotation, ITextComponent message,
            IRenderTypeBuffer bufferIn, int packedLightIn) {
        neo.push();
        neo.rotate(camRotation);
        neo.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = neo.getLast().getMatrix();
        float opacity = Minecraft.getInstance().gameSettings.getTextBackgroundOpacity(0.25F);
        int opacityAsPackedColor = (int) (opacity * 255.0F) << 24;
        FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
        float xOffset = (float) (-fontRenderer.getStringPropertyWidth(message) / 2);
        fontRenderer.func_243247_a(message, xOffset, 0, 553648127, false, matrix4f, bufferIn, false,
                opacityAsPackedColor,
                packedLightIn);
        neo.pop();
    }
}
