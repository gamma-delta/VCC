package me.gammadelta.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.gammadelta.common.block.tile.ContainerPuncher;
import me.gammadelta.common.block.tile.TilePuncher;
import me.gammadelta.common.item.VCCItems;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static me.gammadelta.VCCMod.MOD_ID;

public class GuiPuncher extends ContainerScreen<ContainerPuncher> {
    private ResourceLocation GUI_TEXTURE = new ResourceLocation(MOD_ID, "textures/gui/puncher_container.png");
    private ResourceLocation TEXT_TEXTURE = new ResourceLocation(MOD_ID, "textures/font/monospace.png");

    private Widget copyButton;

    public GuiPuncher(ContainerPuncher container, PlayerInventory inv, ITextComponent name) {
        super(container, inv, name);
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 163;
        this.titleX = 7;
        this.titleY = 4;
        this.xSize = 256;
        this.ySize = 256;
    }

    @Override
    protected void init() {
        super.init();
        this.copyButton = this.addButton(new CopyButton());
    }

    @Override
    public void tick() {
        super.tick();


        // Which top button set should we use?
        byte[] memory = this.container.getMemory();
        ItemStack dataStack = this.container.getSlot(0).getStack();
        if (TilePuncher.itemGetStrings(dataStack) == null) {
            // Use the single copy one
            this.copyButton.visible = true;
            // Disable if empty
            this.copyButton.active = !dataStack.isEmpty();
            // Do we have a message?
            String key = (memory == null) ? "gui.vcc.puncher.copy" : "gui.vcc.puncher.copy.overwrite";
            this.copyButton.setMessage(new TranslationTextComponent(key));
        } else {
            // Use the triple one
            this.copyButton.visible = false;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int mouseX,
            int mouseY) {
        this.renderBackground(matrixStack);
        RenderSystem.clearColor(1f, 1f, 1f, 1f);
        this.minecraft.getTextureManager().bindTexture(GUI_TEXTURE);
        int relX = (this.width - this.xSize) / 2;
        int relY = (this.height - this.ySize) / 2;
        blitSized(matrixStack, relX, relY, 0, 0, this.xSize, this.ySize);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        // Make the mouse show up (I think?)
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack neo, int mouseX, int mouseY) {
        // Automatically draws the container name and player inventory string for us in the superclass, how nice
        super.drawGuiContainerForegroundLayer(neo, mouseX, mouseY);

        // Move the mouse over, because the context here thinks that (0, 0) is the corner of the gui
        mouseX -= (this.width - this.xSize) / 2;
        mouseY -= (this.height - this.ySize) / 2;

        // Render data
        byte[] memory = this.container.getMemory();
        if (memory != null) {
            for (int i = 0; i < memory.length; i++) {
                int byteIdx = this.container.getByteOffset() + i;
                if (byteIdx > memory.length - 1) {
                    // out of bounds!
                    break;
                }
                byte b = memory[byteIdx];

                int gridX = i % 16;
                int gridY = i / 16;

                int hexX = 9 + gridX * 10 + 2 * (gridX / 8);
                int hexY = 39 + gridY * 6 + 3 * (gridY / 8);
                String hex = String.format("%02x", b);
                renderSmolString(neo, hex, hexX, hexY);

                int asciiX = 173 + 4 * gridX;
                int asciiY = hexY;
                String ascii = String.valueOf((char) b);
                assert ascii.length() == 1;
                renderSmolString(neo, ascii, asciiX, asciiY);
            }
            // Render size & viewing info
            renderSmolString(neo, I18n.format("gui.vcc.puncher.bytesInfo.stored"), 172, 143);
            renderSmolString(neo, String.format("0x%1$04x (%1$d)", memory.length), 176, 149);
            renderSmolString(neo, I18n.format("gui.vcc.puncher.bytesInfo.viewing"), 172, 157);
            int bytesShown = Math.min(256, this.container.getByteOffset() + memory.length);
            int bytesEnd = this.container.getByteOffset() + bytesShown;
            renderSmolString(neo, String.format("0x%04x-0x%04x", this.container.getByteOffset(), bytesEnd), 176, 163);
            renderSmolString(neo, String.format("(%d-%d)", this.container.getByteOffset(), bytesEnd), 176, 169);

        } else {
            for (int i = 0; I18n.hasKey("gui.vcc.puncher.noData.line" + i); i++) {
                renderSmolString(neo, I18n.format("gui.vcc.puncher.noData.line" + i), 9, 39 + 6 * i);
            }
        }

        // Render a helpful tooltip for slots if there is no item there
        if (hoveredSlot != null && !hoveredSlot.getHasStack() && hoveredSlot.inventory != playerInventory) {
            int slot = hoveredSlot.getSlotIndex();
            if (slot <= 4) {
                renderTooltip(neo, new TranslationTextComponent("gui.vcc.puncher.slot" + slot), mouseX,
                        mouseY);
            }
        }

    }

    /**
     * Large button at the top, when not split in 3
     */
    @OnlyIn(Dist.CLIENT)
    private class CopyButton extends AbstractButton {
        public CopyButton() {
            super(GuiPuncher.this.guiLeft + 48, GuiPuncher.this.guiTop + 12, 177, 20,
                    new TranslationTextComponent("gui.vcc.puncher.copy"));
        }

        @Override
        public void onPress() {
            // Copy the data from the punch card
            // Hm fun fact, making a non-static class lets you go OuterClass.this?
            // *** the more you know ***
            GuiPuncher owner = GuiPuncher.this;
            ItemStack dataStack = owner.container.getSlot(0).getStack();
            if (dataStack.getItem() == VCCItems.FILLED_PUNCHCARD.get()) {
                // poggers
                byte[] newData = VCCItems.FILLED_PUNCHCARD.get().getMemory(dataStack);
                owner.container.setMemory(newData);
                owner.container.markDirty();
            }
        }

        @Override
        public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY) {
            GuiPuncher owner = GuiPuncher.this;
            ItemStack dataStack = owner.container.getSlot(0).getStack();
            if (dataStack.getItem() != VCCItems.FILLED_PUNCHCARD.get()) {
                // tell the error
                renderTooltip(matrixStack, new TranslationTextComponent("gui.vcc.puncher.copy.noCopyable"), mouseX,
                        mouseY);
            }
        }
    }

    private boolean isIndexInViewport(int index, int byteCount, int offset) {
        // The index must be more than the first byte index shown.
        // We allow for all but one row of blank space, too.
        return index >= offset && index + offset <= byteCount - (256 - 16);
    }

    private void renderSmolString(MatrixStack neo, String s, int x, int y) {
        minecraft.getTextureManager().bindTexture(TEXT_TEXTURE);

        byte[] bytes = s.getBytes();
        for (int idx = 0; idx < bytes.length; idx++) {
            byte b = bytes[idx];
            int charIdx;
            if (b >= 32 && b <= 126) {
                charIdx = b - 32;
            } else {
                charIdx = 95;
            }
            int gridX = charIdx % 16;
            int gridY = charIdx / 16;
            blit(neo, x + idx * 4, y, gridX * 3, gridY * 5, 3, 5, 48, 30);
        }

        minecraft.getTextureManager().bindTexture(GUI_TEXTURE);
    }

    /**
     * Minecraft expects textures to be 256x256 unless told otherwise.
     * This is a helper function to automatically blit with the correct size of the
     * texture (256x384)
     */
    private void blitSized(MatrixStack neo, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
        blit(neo, x, y, uOffset, vOffset, uWidth, vHeight, 256, 384);
    }
}
