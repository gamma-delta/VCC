package me.gammadelta.common.item;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import me.gammadelta.common.utils.BinaryUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static me.gammadelta.VCCMod.MOD_ID;

/**
 * Interface for items that store memory on them.
 * <p>
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
     * Will truncate or zero-extend if needed.
     */
    default void setMemory(ItemStack memoryStorageStack, byte[] memory) {
        checkItemTypesMatch(memoryStorageStack);

        // Truncate or extend if required
        byte[] fixedUp = new byte[this.getMemorySize()];
        System.arraycopy(memory, 0, fixedUp, 0, Math.min(this.getMemorySize(), memory.length));

        memoryStorageStack.getOrCreateTag().putByteArray(MEMORY_KEY, fixedUp);
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
        byte[] memory = this.getMemory(stack);
        int usedMemory = 0;
        // Find the index of the *last* non-zero byte
        for (; usedMemory >= 0; usedMemory--) {
            byte data = memory[usedMemory];
            if (data != 0) {
                // here's our stop
                break;
            }
        }
        tooltip.add(new TranslationTextComponent("tooltip.memoryStored", usedMemory, getMemorySize()));
    }
}
