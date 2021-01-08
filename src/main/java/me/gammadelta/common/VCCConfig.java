package me.gammadelta.common;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * VCCCCCCCCCCCCCCCConfig
 */
public class VCCConfig {
    public static ForgeConfigSpec CONFIG;

    public static ForgeConfigSpec.IntValue FLOODFILL_MAX_FOUND;
    public static ForgeConfigSpec.IntValue FLOODFILL_MAX_SEARCH;

    public static ForgeConfigSpec.IntValue DEBUGOGGLES_SEARCH_RADIUS;
    public static ForgeConfigSpec.IntValue DEBUGOGGLES_DEACTIVATE_RADIUS;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        FLOODFILL_MAX_FOUND = BUILDER.comment(
                "Maximum number of blocks a floodfill will return.",
                "This limits the number of components in a computer."
        ).defineInRange("floodfill_max", 128, 1, 2048);
        FLOODFILL_MAX_SEARCH = BUILDER.comment(
                "Maximum number of blocks a floodfill will search through.",
                "This prevents one floodfill from taking too long."
        ).defineInRange("floodfill_max", 4096, 1, 16384);

        DEBUGOGGLES_SEARCH_RADIUS = BUILDER.comment(
                "Square radius the debugoggles will search for a motherboard in.",
                "Note the search is not done every tick, so it's not *that* laggy.",
                "Still, increase with care."
        ).defineInRange("debugoggles_search_radius", 5, 1, 10);
        DEBUGOGGLES_DEACTIVATE_RADIUS = BUILDER.comment(
                "Square radius the motherboard must remain in for the debugoggles to keep functioning.",
                "Going outside this radius clears the debugoggles' overlay."
        ).defineInRange("debugoggles_search_radius", 16, 1, 64);

        CONFIG = BUILDER.build();
    }

    public static void init() {
        // As far as I know, I don't actually have to do anything in this method.
        // Calling it will make the JVM load the class,
        // and the stuff in the static block interacts with Forge somehow.
    }
}
