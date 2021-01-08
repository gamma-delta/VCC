package me.gammadelta.client.integration.jei;

import me.gammadelta.common.item.ItemCoupon;
import me.gammadelta.common.item.VCCItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

import static me.gammadelta.VCCMod.MOD_ID;

// dang jei you got an annotation and everything!
@JeiPlugin
public class VCCJEIPlugin implements IModPlugin {
    private static final ResourceLocation ID = new ResourceLocation(MOD_ID, "main");

    // I think this is how you register one item with several types?
    @Override
    public void registerItemSubtypes(@Nonnull ISubtypeRegistration r) {
        r.registerSubtypeInterpreter(VCCItems.COUPON.get(), stack -> {
            CompoundNBT tag = stack.getOrCreateTag();
            if (tag.contains(ItemCoupon.COLLECTIBLE_INDEX_KEY)) {
                return "collectible";
            } else if (tag.contains(ItemCoupon.ERROR_KEY)) {
                return "error";
            } else {
                return "broken";
            }
        });
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }
}
