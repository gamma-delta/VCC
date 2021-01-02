package me.gammadelta.datagen;

import net.minecraft.data.CustomRecipeBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.function.Consumer;

import static me.gammadelta.VCCMod.MOD_ID;

public class Recipes extends RecipeProvider {
    public Recipes(DataGenerator gen, ExistingFileHelper efh) {
        super(gen);
    }

    @Override
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {
    }

    private void specialRecipe(Consumer<IFinishedRecipe> consumer, SpecialRecipeSerializer<?> serializer) {
        ResourceLocation name = Registry.RECIPE_SERIALIZER.getKey(serializer);
        CustomRecipeBuilder.customRecipe(serializer)
                .build(consumer, new ResourceLocation(MOD_ID, "dynamic/" + name.getPath()).toString());
    }
}
