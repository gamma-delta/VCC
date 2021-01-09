package me.gammadelta.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.gammadelta.common.block.tile.TileMotherboard;
import me.gammadelta.common.item.ItemDebugoggles;
import me.gammadelta.common.item.VCCItems;
import me.gammadelta.common.program.CPURepr;
import me.gammadelta.common.program.RegisterRepr;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class VCCRenderOverlays {
    @SubscribeEvent
    public static void renderOverlay(RenderWorldLastEvent e) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        // 3 = head slot
        ItemStack headArmor = player.inventory.armorInventory.get(3);
        if (headArmor.getItem() == VCCItems.DEBUGOGGLES.get()) {
            tryRenderComputerInfo(e, player, headArmor);
        }
    }

    private static void tryRenderComputerInfo(RenderWorldLastEvent e, ClientPlayerEntity player,
            ItemStack debugoggles) {

        if (debugoggles.getOrCreateTag().contains(ItemDebugoggles.MOTHERBOARD_POS_KEY)) {
            // Render our info!
            BlockPos motherboardPos = NBTUtil.readBlockPos(
                    debugoggles.getTag().getCompound(ItemDebugoggles.MOTHERBOARD_POS_KEY));
            MatrixStack neo = e.getMatrixStack();
            TileMotherboard motherboard = (TileMotherboard) player.world.getTileEntity(motherboardPos);
            if (motherboard != null) {
                Vector3d cameraPos = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();
                double camX = cameraPos.x - 0.5;
                double camY = cameraPos.y - 0.5;
                double camZ = cameraPos.z - 0.5;

                IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();

                // Render CPU groups
                ArrayList<ArrayList<CPURepr>> cpus = motherboard.getMotherboard().cpus;
                for (int i = 0; i < cpus.size(); i++) {
                    ArrayList<CPURepr> cpuGroup = cpus.get(i);
                    for (CPURepr cpu : cpuGroup) {
                        renderFloatingText(
                                neo, cpu.manifestation,
                                camX, camY, camZ,
                                new TranslationTextComponent("misc.debugoggles.cpu.idx", i),
                                buffer);
                    }
                }

                // Render register blocks
                ArrayList<RegisterRepr> registers = motherboard.getMotherboard().registers;
                for (int i = 0; i < registers.size(); i++) {
                    RegisterRepr repr = registers.get(i);
                    // Average the blocks in the repr to see where to render
                    double avgX = 0;
                    double avgY = 0;
                    double avgZ = 0;
                    for (BlockPos pos : repr.manifestations) {
                        avgX += pos.getX();
                        avgY += pos.getY();
                        avgZ += pos.getZ();
                    }
                    int count = repr.manifestations.length;
                    avgX /= count;
                    avgY /= count;
                    avgZ /= count;

                    // Offset in the Y direction of the *screen*
                    // not globally.
                    Vector3d pitched = new Vector3d(0, 1, 0)
                            .rotatePitch(player.rotationPitch * 3.14f / 180 / 2);
                    Vector3d offsetVec = pitched
                            .rotateYaw(player.rotationYaw * 3.14f / 180 * -1)
                            .scale(0.141); // this scale value looks good
                    renderFloatingText(neo, avgX - offsetVec.x, avgY + offsetVec.y, avgZ - offsetVec.z,
                            camX, camY, camZ,
                            new TranslationTextComponent("misc.debugoggles.register.idx", i), buffer);
                    StringBuilder bob = new StringBuilder("0x");
                    for (byte b : repr.value) {
                        bob.append(String.format("%02x", b));
                    }
                    renderFloatingText(neo, avgX + offsetVec.x, avgY - offsetVec.y, avgZ + offsetVec.z,
                            camX, camY, camZ,
                            new StringTextComponent(bob.toString()), buffer);
                }
            }
        }
    }


    // thanks botnia
    private static void renderFloatingText(MatrixStack ms, double posX, double posY, double posZ, double playerX,
            double playerY, double playerZ,
            ITextComponent msg,
            IRenderTypeBuffer buffers) {
        Minecraft mc = Minecraft.getInstance();
        ms.push();
        ms.translate(posX, posY, posZ);
        // eutro promises me this is required
        ms.translate(-playerX, -playerY, -playerZ);

        ms.rotate(mc.getRenderManager().getCameraOrientation());
        float f1 = 0.016666668F * 1.6F;
        ms.scale(-f1, -f1, f1);
        ms.translate(0.0F, 0F / f1, 0.0F);
        int halfWidth = mc.fontRenderer.getStringWidth(msg.getString()) / 2;

        int light = 0xF000F0; // packed light? this means 100% brightness all the time

        float opacity = Minecraft.getInstance().gameSettings.getTextBackgroundOpacity(0.25F);
        int opacityRGB = (int) (opacity * 255.0F) << 24;
        // Render gray background
        mc.fontRenderer.func_243247_a(msg, -halfWidth, 0, 0x20FFFFFF, false, ms.getLast().getMatrix(), buffers, true,
                opacityRGB, light);
        // Render foreground
        mc.fontRenderer.func_243247_a(msg, -halfWidth, 0, 0xFFFFFFFF, false, ms.getLast().getMatrix(), buffers, true,
                0, light);
        ms.pop();
    }

    private static void renderFloatingText(MatrixStack neo, BlockPos pos, double playerX, double playerY,
            double playerZ, TranslationTextComponent message, IRenderTypeBuffer.Impl bufferSource) {
        renderFloatingText(neo, pos.getX(), pos.getY(), pos.getZ(), playerX, playerY, playerZ, message, bufferSource);
    }
}
