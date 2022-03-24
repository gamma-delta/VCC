package me.gammadelta.vcc.common.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemFilledPunchCard extends Item implements IMemoryStorageItem {
    public static final String NAME = "filled_punchcard";

    public ItemFilledPunchCard() {
        // purposely no group
        super(new Item.Properties().maxStackSize(1));
    }

    @Override
    public int getMemorySize() {
        return 256;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip,
            ITooltipFlag flagIn) {
        this.addHexdumpTooltip(stack, worldIn, tooltip, flagIn);
    }
}
