package me.gammadelta.vcc.common.recipe;

import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static me.gammadelta.vcc.VCCMod.MOD_ID;

public class VCCRecipes {
    private static final DeferredRegister<IRecipeSerializer<?>> RECIPES = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);


    public static void register() {
        RECIPES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
