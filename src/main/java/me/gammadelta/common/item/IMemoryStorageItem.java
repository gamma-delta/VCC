package me.gammadelta.common.item;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import me.gammadelta.common.utils.Utils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Interface for items that store memory on them.
 *
 * As a modder, you should just need to do two things:
 * * Implement {@code getMemorySize}
 * * Call {@code addHexdumpTooltip} in the {@code addInformation} method override
 */
public interface IMemoryStorageItem {
    String MEMORY_KEY = "memory";
    String CACHED_HEXDUMP_KEY = "cached_hexdump";


    int getMemorySize();

    /**
     * Get the memory inside this by deserializing it from NBT.
     */
    default byte[] getMemory(ItemStack memoryStorageStack) {
        checkItemTypesMatch(memoryStorageStack);

        byte[] memory = memoryStorageStack.getOrCreateTag().getByteArray(MEMORY_KEY);
        if (memory.length == 0) {
            // probably there was no such memory
            memory = new byte[this.getMemorySize()];
        }
        return memory;
    }

    /**
     * Set the memory inside this by serializing it to NBT.
     */
    default void setMemory(ItemStack memoryStorageStack, byte[] memory) {
        checkItemTypesMatch(memoryStorageStack);

        memoryStorageStack.getOrCreateTag().putByteArray(MEMORY_KEY, memory);
    }

    /**
     * Check the item types match up
     */
    default void checkItemTypesMatch(ItemStack stack) {
        Item stacksItem = stack.getItem();
        if (stacksItem != this) {
            throw new IllegalArgumentException(
                    String.format("This has type %s but passed in %s", this.getClass().getSimpleName(),
                            stacksItem.getClass().getSimpleName()));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @ParametersAreNonnullByDefault
    default void addHexdumpTooltip(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip,
            ITooltipFlag flagIn) {
        if (!Screen.hasShiftDown()) {
            tooltip.add(new TranslationTextComponent("tooltip.hexdump.hidden", getMemorySize()));
            stack.getOrCreateTag().remove(CACHED_HEXDUMP_KEY);
        } else {
            String hexdump = stack.getOrCreateTag().getString(CACHED_HEXDUMP_KEY);
            if (hexdump.equals("")) {
                // we don't know about this hexdump
                // all this caching is to prevent running the hexdump every frame.
                // hopefully it's worth it...
                hexdump = Utils.hexdump(new ByteArrayList(this.getMemory(stack)));
                stack.getOrCreateTag().putString(CACHED_HEXDUMP_KEY, hexdump);
            }

            tooltip.add(new TranslationTextComponent("tooltip.hexdump.shown", hexdump,
                    I18n.format("item.vcc.clipboard")));
        }
    }
}
