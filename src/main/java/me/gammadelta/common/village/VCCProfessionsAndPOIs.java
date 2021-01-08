package me.gammadelta.common.village;

import com.google.common.collect.ImmutableSet;
import me.gammadelta.common.block.BlockPuncher;
import me.gammadelta.common.block.VCCBlocks;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.util.SoundEvents;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static me.gammadelta.VCCMod.MOD_ID;

public class VCCProfessionsAndPOIs {
    private static final DeferredRegister<PointOfInterestType> POINTS_OF_INTEREST = DeferredRegister.create(
            ForgeRegistries.POI_TYPES, MOD_ID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(
            ForgeRegistries.PROFESSIONS, MOD_ID);

    // region POIs

    public static final RegistryObject<PointOfInterestType> PUNCHER_POI = POINTS_OF_INTEREST.register(BlockPuncher.NAME,
            () -> new PointOfInterestType(BlockPuncher.NAME,
                    PointOfInterestType.getAllStates(VCCBlocks.PUNCHER_BLOCK.get()), 1, 1));

    // endregion

    // region Professions

    public static final RegistryObject<VillagerProfession> PROGRAMMER = PROFESSIONS.register("programmer",
            () -> new VillagerProfession("programmer", PUNCHER_POI.get(),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.BLOCK_PISTON_EXTEND));

    // endregion

    public static void register() {
        POINTS_OF_INTEREST.register(FMLJavaModLoadingContext.get().getModEventBus());
        PROFESSIONS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
