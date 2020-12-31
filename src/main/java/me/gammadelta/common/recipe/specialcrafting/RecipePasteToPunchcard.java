package me.gammadelta.common.recipe.specialcrafting;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import me.gammadelta.common.item.VCCItems;
import me.gammadelta.common.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

public class RecipePasteToPunchcard extends SpecialRecipe {
    public static final SpecialRecipeSerializer<RecipePasteToPunchcard> SERIALIZER = new SpecialRecipeSerializer<>(
            RecipePasteToPunchcard::new);

    public RecipePasteToPunchcard(ResourceLocation idIn) {
        super(idIn);
    }


    @Override
    @ParametersAreNonnullByDefault
    public boolean matches(CraftingInventory inv, World worldIn) {
        boolean foundClipboard = false;
        boolean foundPunchcard = false;
        boolean foundString = false;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() == VCCItems.CLIPBOARD.get()) {
                    if (foundClipboard) {
                        // two clipboards
                        return false;
                    } else {
                        foundClipboard = true;
                    }
                } else if (stack.getItem() == VCCItems.PUNCHCARD.get()) {
                    if (foundPunchcard) {
                        // two punchcards
                        return false;
                    } else {
                        foundPunchcard = true;
                    }
                } else if (stack.getItem() == Items.STRING) {
                    if (foundString) {
                        // two strings
                        return false;
                    } else {
                        foundString = true;
                    }
                }
            }
        }

        return foundClipboard && foundPunchcard && (attemptToReadClipboardData(foundString) != null);
        // string is not required, so we don't && it here.
    }

    @Override
    @ParametersAreNonnullByDefault
    public ItemStack getCraftingResult(CraftingInventory inv) {
        // When this method is called we know the crafting table already
        // has the right items in it.
        ItemStack out = new ItemStack(VCCItems.FILLED_PUNCHCARD.get(), 1);

        boolean isString = IntStream.range(0, 9).anyMatch(idx -> {
            ItemStack stack = inv.getStackInSlot(idx);
            return stack.getItem() == Items.STRING;
        });
        // this might be null still if the player is being irritating
        // and swapping their clipboard on us really fast
        byte[] data = attemptToReadClipboardData(isString);
        if (data == null) {
            // this shouldn't happen but still
            return ItemStack.EMPTY;
        } else {
            VCCItems.FILLED_PUNCHCARD.get().setMemory(out, data);
        }

        return out;
    }

    @Override
    public boolean canFit(int width, int height) {
        // 1x2, 2x1, and anything larger is OK.
        return (width * height) >= 2;
    }

    @Override
    @ParametersAreNonnullByDefault
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Nonnull
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
        NonNullList<ItemStack> items = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.getItem() == VCCItems.CLIPBOARD.get()) {
                items.set(i, stack);
            }
        }

        return items;
    }

    /**
     * return null if it can't
     */
    private byte[] attemptToReadClipboardData(boolean useString) {
        String clipboard = Minecraft.getInstance().keyboardListener.getClipboardString();
        if (useString) {
            // this actually makes our job very easy
            return clipboard.getBytes(StandardCharsets.UTF_8);
        } else {
            String[] split = clipboard.split("[,;\\s]");
            ByteList out = new ByteArrayList(split.length);
            for (String word : split) {
                try {
                    int trying = Utils.parseInt(word);
                    if (trying <= 0xff) {
                        out.add((byte) trying);
                    } else {
                        return null;
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return out.toByteArray();
        }
    }
}
