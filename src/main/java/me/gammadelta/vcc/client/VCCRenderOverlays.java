package me.gammadelta.vcc.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import it.unimi.dsi.fastutil.ints.IntList;
import me.gammadelta.vcc.common.block.tile.TileMotherboard;
import me.gammadelta.vcc.common.item.ItemDebugoggles;
import me.gammadelta.vcc.common.item.VCCItems;
import me.gammadelta.vcc.common.program.CPURepr;
import me.gammadelta.vcc.common.program.MotherboardRepr;
import me.gammadelta.vcc.common.program.RegisterRepr;
import me.gammadelta.vcc.common.utils.BinaryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
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
            CompoundNBT tag = debugoggles.getOrCreateTag();
            BlockPos motherboardPos = NBTUtil.readBlockPos(tag.getCompound(ItemDebugoggles.MOTHERBOARD_POS_KEY));
            MatrixStack neo = e.getMatrixStack();
            TileMotherboard motherboard = (TileMotherboard) player.world.getTileEntity(motherboardPos);
            if (motherboard != null) {
                if (!tag.contains(ItemDebugoggles.CPU_POS_KEY)) {
                    renderInfoWRTMotherboard(neo, motherboard, player);
                } else {
                    BlockPos cpuPos = NBTUtil.readBlockPos(tag.getCompound(ItemDebugoggles.CPU_POS_KEY));
                    MotherboardRepr motherRepr = motherboard.getMotherboard();
                    ArrayList<ArrayList<CPURepr>> cpus = motherRepr.cpus;
                    groups:
                    for (int i = 0; i < cpus.size(); i++) {
                        ArrayList<CPURepr> cpuGroup = cpus.get(i);
                        for (CPURepr candidate : cpuGroup) {
                            if (candidate.manifestation.equals(cpuPos)) {
                                // nice
                                renderInfoWRTCpu(neo, candidate, i, motherboard, player);
                                break groups;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void renderInfoWRTMotherboard(MatrixStack neo, TileMotherboard motherboard,
            ClientPlayerEntity player) {
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
                        neo,
                        cpu.manifestation.getX(), cpu.manifestation.getY(), cpu.manifestation.getZ(),
                        camX, camY, camZ,
                        player.rotationPitch, player.rotationYaw,
                        buffer,
                        new TranslationTextComponent("misc.debugoggles.cpu.idx", i));
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

            StringBuilder bob = new StringBuilder("0x");
            for (byte b : repr.value) {
                bob.append(String.format("%02x", b));
            }
            bob.append(String.format(" (%d)", repr.getByteCount()));
            renderFloatingText(neo, avgX, avgY, avgZ, camX, camY, camZ, player.rotationPitch,
                    player.rotationYaw,
                    buffer,
                    new TranslationTextComponent("misc.debugoggles.register.idx", i),
                    new StringTextComponent(bob.toString())
            );
        }
    }

    private static void renderInfoWRTCpu(MatrixStack neo, CPURepr cpu, int cpuGroupIdx, TileMotherboard tileMother,
            ClientPlayerEntity player) {
        Vector3d cameraPos = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();
        double camX = cameraPos.x - 0.5;
        double camY = cameraPos.y - 0.5;
        double camZ = cameraPos.z - 0.5;
        IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();

        MotherboardRepr mother = tileMother.getMotherboard();

        // Render CPU
        renderFloatingText(neo, cpu.manifestation.getX(), cpu.manifestation.getY(), cpu.manifestation.getZ(),
                camX, camY, camZ,
                player.rotationPitch, player.rotationYaw,
                buffer,
                new TranslationTextComponent("misc.debugoggles.cpu.selected"),
                new TranslationTextComponent("misc.debugoggles.cpu.idx", cpuGroupIdx),
                new TranslationTextComponent("misc.debugoggles.cpu.ipsp", BinaryUtils.toLong(cpu.IP),
                        BinaryUtils.toLong(cpu.SP))
        );


        // Render registers
        for (int regiGroupIdx = 0; regiGroupIdx < cpu.registers.size(); regiGroupIdx++) {
            for (int regiIdx = 0; regiIdx < cpu.registers.get(regiGroupIdx).size(); regiIdx++) {
                int absoluteRegiIdx = cpu.registers.get(regiGroupIdx).getInt(regiIdx);
                RegisterRepr regi = mother.registers.get(absoluteRegiIdx);
                // Average the blocks in the repr to see where to render
                double avgX = 0;
                double avgY = 0;
                double avgZ = 0;
                for (BlockPos pos : regi.manifestations) {
                    avgX += pos.getX();
                    avgY += pos.getY();
                    avgZ += pos.getZ();
                }
                int count = regi.manifestations.length;
                avgX /= count;
                avgY /= count;
                avgZ /= count;

                StringBuilder bob = new StringBuilder("0x");
                for (byte b : regi.value) {
                    bob.append(String.format("%02x", b));
                }
                bob.append(String.format(" (%d)", regi.getByteCount()));
                renderFloatingText(neo, avgX, avgY, avgZ, camX, camY, camZ, player.rotationPitch,
                        player.rotationYaw,
                        buffer,
                        new StringTextComponent(String.format("R%d", regiGroupIdx)),
                        new StringTextComponent(bob.toString())
                );
            }
        }

        // Render memory
        // i apologize in advance to anyone who has to debug this indexIndex index indexidx Index idxcies
        cpu.memoryLocations.forEach((memType, indexBatches) -> {
            int blocksProcessed = 0;
            for (int batchIdx = 0; batchIdx < indexBatches.size(); batchIdx++) {
                IntList indexesBatch = indexBatches.get(batchIdx);
                for (int memBlockIdx : indexesBatch) {
                    BlockPos pos = mother.memoryLocations.get(memType).get(memBlockIdx);

                    long memoryStart = cpu.memoryStarts.get(memType) + blocksProcessed * memType.storageAmount;
                    long memoryEnd = cpu.memoryStarts.get(
                            memType) + (blocksProcessed + indexesBatch.size()) * memType.storageAmount - 1;

                    String transKey = "misc.debugoggles.memory." + memType.name();
                    renderFloatingText(neo, pos.getX(), pos.getY(), pos.getZ(), camX, camY, camZ, player.rotationPitch,
                            player.rotationYaw,
                            buffer,
                            new TranslationTextComponent(transKey, batchIdx),
                            new StringTextComponent(String.format("0x%x-0x%x", memoryStart, memoryEnd)),
                            new StringTextComponent(String.format("(%d-%d)", memoryStart, memoryEnd))
                    );
                }
                blocksProcessed += indexBatches.size();
            }
        });
    }


    /**
     * Render a list of floating text.
     * They are rendered top-to-bottom in screen space.
     */
    private static void renderFloatingText(MatrixStack ms, double posX, double posY, double posZ, double eyeX,
            double eyeY, double eyeZ, float rotationPitch, float rotationYaw,
            IRenderTypeBuffer buffers,
            ITextComponent... messages) {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < messages.length; i++) {
            ms.push();

            ms.translate(posX, posY, posZ);
            // eutro promises me this is required
            ms.translate(-eyeX, -eyeY, -eyeZ);

            float f1 = 0.016666668F * 1.6F;

            // Offset in the Y direction of the *screen*
            // not globally.
            float centeredIdx = i - (messages.length - 1) / 2f;
            Vector3d pitched = new Vector3d(0, 1, 0)
                    .rotatePitch(rotationPitch * 3.14f / 180 / 2);
            Vector3d offsetVec = pitched
                    .rotateYaw(rotationYaw * 3.14f / 180)
                    .scale(0.3 * centeredIdx); // this scale value looks good
            ms.translate(offsetVec.x, -offsetVec.y, offsetVec.z);

            ms.rotate(mc.getRenderManager().getCameraOrientation());
            ms.scale(-f1, -f1, f1);

            ITextComponent msg = messages[i];
            int halfWidth = mc.fontRenderer.getStringWidth(msg.getString()) / 2;

            int light = 0xF000F0; // packed light? this means 100% brightness all the time

            float opacity = Minecraft.getInstance().gameSettings.getTextBackgroundOpacity(0.25F);
            int opacityRGB = (int) (opacity * 255.0F) << 24;
            // Render gray background
            mc.fontRenderer.func_243247_a(msg, -halfWidth, 0, 0x20FFFFFF, false, ms.getLast().getMatrix(), buffers,
                    true,
                    opacityRGB, light);
            // Render foreground
            mc.fontRenderer.func_243247_a(msg, -halfWidth, 0, 0xFFFFFFFF, false, ms.getLast().getMatrix(), buffers,
                    true,
                    0, light);

            ms.pop();
        }
    }
}
