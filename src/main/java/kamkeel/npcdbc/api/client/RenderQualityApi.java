package kamkeel.npcdbc.api.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.npcdbc.client.RenderSettings;
import kamkeel.npcdbc.client.shader.GlCaps;
import kamkeel.npcdbc.client.shader.PostProcessing;

/**
 * Stable entry point for performance mods (DbrOptimizer) that tune NpcDbc's rendering cost.
 * Meant to be called by reflection, so it only takes and returns strings, ints and booleans;
 * add methods, never rename or change them. Check {@link #API_VERSION} before using newer ones.
 *
 * <p>Presets: {@code HIGH} (original look), {@code MEDIUM}, {@code LOW}, {@code VERY_LOW}. A preset set
 * here replaces AUTO but never what the player fixed in client.cfg (a chosen Graphics Quality or a
 * manual option). Nothing set here is saved.
 */
@SideOnly(Side.CLIENT)
public final class RenderQualityApi {

    public static final int API_VERSION = 1;

    private RenderQualityApi() {}

    /** Asks for a preset by name; null, "" or "AUTO" hands the choice back to NpcDbc. */
    public static void setExternalPreset(String preset) {
        RenderSettings.setExternal(RenderSettings.Quality.parse(preset));
    }

    /** The preset in use right now, after the player's choice, the external one and AUTO. */
    public static String getEffectivePreset() {
        return RenderSettings.effective().name();
    }

    /**
     * Bloom cost on top of the preset: 0 = full, 1 = light (blur chain at a quarter of the screen),
     * 2 = off. Only ever lowers what the config allows.
     */
    public static void setBloomLevel(int level) {
        PostProcessing.bloomLevel = Math.max(0, Math.min(2, level));
    }

    /** The main framebuffer has a stencil buffer (auras get the body mask, outlines the full silhouette). */
    public static boolean hasStencil() {
        return PostProcessing.hasInitialized && GlCaps.stencil;
    }

    /** The GPU passed every check the bloom chain needs. */
    public static boolean isBloomSupported() {
        return PostProcessing.bloomSupported;
    }
}
