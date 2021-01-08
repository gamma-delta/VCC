package me.gammadelta.common.item;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.List;

import static me.gammadelta.VCCMod.MOD_ID;

public class ItemCoupon extends Item {
    public static final String NAME = "coupon";

    /**
     * If this key is present, it's collectible.
     */
    public static final String COLLECTIBLE_INDEX_KEY = "collectible_index";
    /**
     * If this key is present, it's an error.
     */
    public static final String ERROR_KEY = "errors";

    public static final String LEX_KEY = "lex";
    public static final String PREPROCESS_KEY = "preprocess";
    public static final String PARSE_KEY = "parse";
    public static final String BYTECODE_KEY = "bytecode";

    public static final String[] ERROR_TYPES = new String[]{
            LEX_KEY,
            PREPROCESS_KEY,
            PARSE_KEY,
            BYTECODE_KEY,
    };

    public static final String ERROR_KEY_KEY = "key";
    public static final String ERROR_VALUES_KEY = "values";
    public static final String ERROR_LINE_KEY = "line";
    public static final String ERROR_ROW_KEY = "row";
    public static final String ERROR_COL_KEY = "col";

    public static final int MAX_COLLECTIBLE_IDX = 7;

    public static final ResourceLocation COUPON_STATE_PREDICATE = new ResourceLocation(MOD_ID, "coupon_state");

    public ItemCoupon() {
        // Purposely do not put it into a group
        super(new Properties().maxStackSize(1));
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        if (tag.contains(COLLECTIBLE_INDEX_KEY)) {
            return "item.vcc.coupon.collectible";
        } else if (tag.contains(ERROR_KEY)) {
            return "item.vcc.coupon.compilation";
        } else {
            return "item.vcc.coupon.borked";
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip,
            ITooltipFlag flagIn) {
        CompoundNBT tag = stack.getOrCreateTag();
        if (tag.contains(COLLECTIBLE_INDEX_KEY)) {
            int index = tag.getInt(COLLECTIBLE_INDEX_KEY);

            tooltip.add(new TranslationTextComponent("item.vcc.coupon.collectible.tooltip", index + 1,
                    MAX_COLLECTIBLE_IDX + 1));

            String transKey = "item.vcc.coupon.collectible." + index;
            if (I18n.hasKey(transKey)) {
                tooltip.add(new TranslationTextComponent(transKey));
            } else {
                tooltip.add(new TranslationTextComponent("item.vcc.coupon.collectible.heck"));
            }

        } else if (tag.contains(ERROR_KEY)) {
            CompoundNBT errors = tag.getCompound(ERROR_KEY);
            /*
            errors: {
                lex: [{line:"FOO BAR BAZ" row: 12, col: 34, key: "error.lex.abc", values: ["foo", "bar", "baz"]}]
                preprocess: [ etc ]
                compile: [ etc ]
            }
             */
            for (String errorType : ERROR_TYPES) {
                String masterKey = "item.vcc.coupon.compilation.error." + errorType;
                if (errors.contains(masterKey)) {
                    tooltip.add(new TranslationTextComponent("item.vcc.coupon.compilation.error.lex"));
                    ListNBT problems = errors.getList(masterKey, Constants.NBT.TAG_COMPOUND);
                    for (int i = 0; i < problems.size(); i++) {
                        CompoundNBT prob = problems.getCompound(i);
                        String key = prob.getString(ERROR_KEY_KEY); //'s delivery service
                        ListNBT valsNBT = prob.getList(ERROR_VALUES_KEY, Constants.NBT.TAG_STRING);
                        String[] vals = new String[valsNBT.size()];
                        for (int j = 0; j < valsNBT.size(); j++) {
                            vals[j] = valsNBT.getString(j);
                        }
                        String errorMessage = I18n.format(key, (Object[]) vals);

                        int row = prob.getInt(ERROR_ROW_KEY);
                        int col = prob.getInt(ERROR_COL_KEY);
                        String lineContents = prob.getString(ERROR_LINE_KEY);
                        tooltip.add(
                                new TranslationTextComponent("item.vcc.coupon.compilation.rowTemplate", lineContents,
                                        row, col, errorMessage));
                    }
                }
            }
        }
    }
}
