package me.gammadelta.common.recipe;

import me.gammadelta.common.recipe.specialcrafting.RecipePasteToPunchcard;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import static me.gammadelta.VCCMod.MOD_ID;

public class VCCRecipes {
    private static final DeferredRegister<IRecipeSerializer<?>> RECIPES = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);

    public static final RegistryObject<IRecipeSerializer<?>> PASTE_TO_PUNCHCARD = RECIPES.register("paste_to_punchcard",
            RecipePasteToPunchcard.SERIALIZER.delegate);

    public static void register() {
        RECIPES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
