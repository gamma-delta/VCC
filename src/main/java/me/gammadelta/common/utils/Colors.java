package me.gammadelta.common.utils;

import static net.minecraft.util.ColorHelper.PackedColor.packColor;

/**
 * Constant colors
 */
public class Colors {
    /**
     * Red for Red-gisters
     */
    public static int REGISTER_RED = packColor(255, 255, 0, 0);
    /**
     * Sky-blue for IP extenders
     */
    public static int IP_EXTENDER_BLUE = packColor(255, 0, 150, 255);
    /**
     * Orange for SP extenders
     * this is the same orange as the `public static int` in intellij
     */
    public static int SP_EXTENDER_ORANGE = packColor(255, 0xe0, 0x95, 0x7b);
}
