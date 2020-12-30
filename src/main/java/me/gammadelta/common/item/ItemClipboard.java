package me.gammadelta.common.item;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

import static me.gammadelta.common.item.VCCItems.VCC_ITEM_GROUP;

public class ItemClipboard extends Item {
    public static final String NAME = "clipboard";

    public ItemClipboard() {
        super(new Item.Properties().group(VCC_ITEM_GROUP).maxStackSize(1));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip,
            ITooltipFlag flagIn) {
        if (Screen.hasShiftDown()) {
            for (int i = 0; i <= 4; i++) {
                tooltip.add(new TranslationTextComponent("item.vcc.clipboard.tooltip" + i));
            }
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.shiftForMore"));
        }

    }
}
