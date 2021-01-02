package me.gammadelta.client.gui;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.fonts.IGlyphInfo;
import net.minecraft.client.gui.fonts.providers.IGlyphProvider;
import net.minecraft.client.gui.fonts.providers.IGlyphProviderFactory;
import net.minecraft.client.gui.fonts.providers.TextureGlyphProvider;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

import java.io.IOException;

import static me.gammadelta.VCCMod.MOD_ID;

/**
 * Provides the monospace font, redirecting
 * invalid code points to the center dot.
 *
 * Also my first foray into ATs! We need to make `texture` and `glyphInfos` public.
 */
public class VCCMonospaceGlyphProvider extends TextureGlyphProvider {
    protected IGlyphInfo defaultGlyphInfo;

    private VCCMonospaceGlyphProvider(NativeImage image, Int2ObjectMap<TextureGlyphProvider.GlyphInfo> glyphInfos, GlyphInfo defaultGlyphInfo) {
        super(image, glyphInfos);
        this.defaultGlyphInfo = defaultGlyphInfo;
    }

    @Nullable
    @Override
    public IGlyphInfo getGlyphInfo(int character) {
        GlyphInfo res = this.glyphInfos.get(character);
        if (res == null) {
            // o no return the center dot
            return this.defaultGlyphInfo;
        }
        return res;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements IGlyphProviderFactory {
        @Nullable
        @Override
        public IGlyphProvider create(IResourceManager manager) {
            try (IResource resource = manager.getResource(new ResourceLocation(MOD_ID, "texture/font/monospace.png"))) {
                NativeImage image = NativeImage.read(NativeImage.PixelFormat.RGBA, resource.getInputStream());
                int width = image.getWidth();
                int height = image.getHeight();

                Int2ObjectMap<TextureGlyphProvider.GlyphInfo> glyphInfos = new Int2ObjectOpenHashMap<>();
                // This bitmap has 72 characters on it.
                for (int idx = 0; idx < 72; idx++) {
                    int character = idx + 20; // the first character is a space, char #20
                    int gridX = idx % 16;
                    int gridY = idx / 16;
                    glyphInfos.put(character, new GlyphInfo(
                            1f, // vertical scale?
                            image,
                            3 * gridX, // srcX
                            5 * gridY, // srcY
                            3, // width
                            5, // height
                            0, // i got no clue atm
                            5 // ascent, aka how many pixels above baseline things should be drawn from
                    ));
                }

                return new VCCMonospaceGlyphProvider(image, glyphInfos, new GlyphInfo(
                        1f, image, 45, 25, 3, 5, 0, 5
                ));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
