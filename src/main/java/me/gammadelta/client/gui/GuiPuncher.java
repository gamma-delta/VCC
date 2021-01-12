package me.gammadelta.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.gammadelta.VCCMod;
import me.gammadelta.common.block.tile.ContainerPuncher;
import me.gammadelta.common.block.tile.TilePuncher;
import me.gammadelta.common.item.VCCItems;
import me.gammadelta.common.network.MsgCompile;
import me.gammadelta.common.network.MsgPunch;
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
import net.minecraftforge.items.SlotItemHandler;

import java.util.List;

import static me.gammadelta.VCCMod.MOD_ID;

public class GuiPuncher extends ContainerScreen<ContainerPuncher> {
    private ResourceLocation GUI_TEXTURE = new ResourceLocation(MOD_ID, "textures/gui/puncher_container.png");
    private ResourceLocation TEXT_TEXTURE = new ResourceLocation(MOD_ID, "textures/font/monospace.png");

    private Widget copyButton;
    private Widget copyStringButton;
    private Widget compileStringButton;

    private Widget punchButton;

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
        this.copyStringButton = this.addButton(new SmolDataInputButton(SmolDataInputButtonType.STRING));
        this.compileStringButton = this.addButton(new SmolDataInputButton(SmolDataInputButtonType.COMPILE));

        this.punchButton = this.addButton(new PunchButton());
    }

    @Override
    public void tick() {
        super.tick();
        // Which top button set should we use?
        byte[] memory = this.container.getMemory();
        ItemStack dataStack = this.container.getSlot(0).getStack();
        if (TilePuncher.getStringDataFromItem(dataStack) == null) {
            // Use the single copy one
            this.copyButton.visible = true;
            this.copyStringButton.visible = false;
            this.compileStringButton.visible = false;

            // Disable if empty
            this.copyButton.active = !dataStack.isEmpty();
            // Do we have a message?
            String key = (memory == null) ? "gui.vcc.puncher.copy" : "gui.vcc.puncher.copy.overwrite";
            this.copyButton.setMessage(new TranslationTextComponent(key));
        } else {
            // Use the triple one
            this.copyButton.visible = false;
            this.copyStringButton.visible = true;
            this.compileStringButton.visible = true;

            // Disable all 3 if empty
            boolean active = !dataStack.isEmpty();
            this.copyStringButton.active = active;
            this.compileStringButton.active = active;
            if (active) {
                // Disable compilation if there's something in the error output
                this.compileStringButton.active = !this.container.getSlot(2).getHasStack() && this.container.getSlot(1)
                        .getHasStack();
            }
        }

        this.punchButton.active = this.container.getMemory() != null && this.container.getSlot(3).getHasStack();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack neo, float partialTicks, int mouseX,
            int mouseY) {
        this.renderBackground(neo);
        RenderSystem.clearColor(1f, 1f, 1f, 1f);
        // Draw gui base
        this.minecraft.getTextureManager().bindTexture(GUI_TEXTURE);
        int relX = (this.width - this.xSize) / 2;
        int relY = (this.height - this.ySize) / 2;
        blitSized(neo, relX, relY, 0, 0, this.xSize, this.ySize);

        // Draw the coverer over the shadow that shows you what items can go places
        if (this.container.getSlot(1).getHasStack()) {
            blitSized(neo, relX + 29, relY + 13, 9, 256, 18, 18);
        }
        if (this.container.getSlot(3).getHasStack()) {
            blitSized(neo, relX + 7, relY + 142, 9, 256, 18, 18);
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        // Make the mouse icon show up (I think?)
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
            for (int i = 0; i < Math.min(memory.length, 256); i++) {
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
        if (hoveredSlot != null && !hoveredSlot.getHasStack() && hoveredSlot instanceof SlotItemHandler) {
            // the instanceof check is because in the container,
            // all the player slots are Slots and all the gui slots are SlotItemHandlers.
            // This is a little hacky but it works and is very simple.
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

    /**
     * The 3 small buttons at the top
     */
    @OnlyIn(Dist.CLIENT)
    private class SmolDataInputButton extends AbstractButton {
        SmolDataInputButtonType type;

        public SmolDataInputButton(SmolDataInputButtonType type) {
            super(GuiPuncher.this.guiLeft + type.x, GuiPuncher.this.guiTop + type.y, 58, 20,
                    new TranslationTextComponent(type.getButtonNameKey()));
            this.type = type;
        }

        @Override
        public void onPress() {
            ItemStack dataStack = GuiPuncher.this.container.getSlot(0).getStack();
            List<String> stringData = TilePuncher.getStringDataFromItem(dataStack);
            if (stringData == null) {
                // :HOW:
                return;
            }
            String combinedData = String.join("\n", stringData);
            if (this.type == SmolDataInputButtonType.STRING) {
                // nice!
                byte[] newMemory = combinedData.getBytes();
                GuiPuncher.this.container.setMemory(newMemory);
            } else if (this.type == SmolDataInputButtonType.LITERALS) {
                // TODO
            } else {
                // Compiling time~~
                VCCMod.getNetwork()
                        .sendToServer(
                                new MsgCompile(GuiPuncher.this.container.windowId, stringData.toArray(new String[0])));
            }
        }
    }

    private enum SmolDataInputButtonType {
        STRING(48, 12), LITERALS(107, 12), COMPILE(166, 12);
        int x, y;

        SmolDataInputButtonType(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public String getButtonNameKey() {
            switch (this) {
                case STRING:
                    return "gui.vcc.puncher.copy.string";
                case LITERALS:
                    return "gui.vcc.puncher.copy.literals";
                case COMPILE:
                    return "gui.vcc.puncher.compile";
                default:
                    return "i hate java";
            }
        }
    }

    /**
     * Button that punches data to a card
     */
    private class PunchButton extends AbstractButton {
        public PunchButton() {
            super(GuiPuncher.this.guiLeft + 26, GuiPuncher.this.guiTop + 141, 143, 20,
                    new TranslationTextComponent("gui.vcc.puncher.punch"));


        }

        @Override
        public void onPress() {
            VCCMod.getNetwork().sendToServer(new MsgPunch(GuiPuncher.this.container.windowId));
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
        blit(neo, x, y, uOffset, vOffset, uWidth, vHeight, 256, 298);
    }
}
