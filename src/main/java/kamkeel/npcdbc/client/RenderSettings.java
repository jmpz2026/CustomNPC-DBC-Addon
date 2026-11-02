package kamkeel.npcdbc.client;

import kamkeel.npcdbc.client.shader.GlCaps;
import kamkeel.npcdbc.client.shader.PostProcessing;
import kamkeel.npcdbc.config.ConfigDBCClient;

/**
 * Turns the quality preset and the manual options of client.cfg into the values the renderer reads
 * ({@code ConfigDBCClient.EnableBloom}, {@code AuraMaxDistance}, ...).
 *
 * <p>Who wins, from strongest to weakest:
 * <ol>
 *   <li>a manual value in client.cfg (anything other than "preset" / -1);</li>
 *   <li>the "Graphics Quality" the player picked, unless it is AUTO;</li>
 *   <li>the preset another mod asked for through {@code RenderQualityApi} (DbrOptimizer's light mode);</li>
 *   <li>AUTO: what the GPU can do ({@link GlCaps}).</li>
 * </ol>
 */
public final class RenderSettings {

    public enum Quality {
        //        bloom  outlines faceDist auraDist outlineDist particles lowRes levels
        HIGH(true, true, 0, 0, 0, 100, false, 0),
        MEDIUM(true, true, 32, 48, 48, 75, true, 4),
        LOW(false, false, 24, 32, 32, 50, true, 3),
        VERY_LOW(false, false, 16, 16, 16, 25, true, 2);

        final boolean bloom, outlines, bloomLowRes;
        final int modelDetail, aura, outline, particles, bloomLevels;

        Quality(boolean bloom, boolean outlines, int modelDetail, int aura, int outline, int particles, boolean bloomLowRes, int bloomLevels) {
            this.bloom = bloom;
            this.outlines = outlines;
            this.modelDetail = modelDetail;
            this.aura = aura;
            this.outline = outline;
            this.particles = particles;
            this.bloomLowRes = bloomLowRes;
            this.bloomLevels = bloomLevels;
        }

        /** Null for AUTO or anything unknown. */
        public static Quality parse(String name) {
            if (name == null)
                return null;
            for (Quality q : values())
                if (q.name().equalsIgnoreCase(name.trim()))
                    return q;
            return null;
        }
    }

    public static final String AUTO = "AUTO";
    public static final String FOLLOW = "preset", ON = "on", OFF = "off";

    private static volatile Quality external;
    private static Quality effective = Quality.HIGH;

    private RenderSettings() {}

    public static Quality effective() {
        return effective;
    }

    public static void setExternal(Quality quality) {
        external = quality;
        apply();
    }

    /** What AUTO means on this GPU. Before the first {@link PostProcessing#init} nothing is known yet: HIGH. */
    public static Quality auto() {
        if (!PostProcessing.hasInitialized)
            return Quality.HIGH;
        if (!GlCaps.fbo || !GlCaps.shaders)
            return Quality.VERY_LOW;
        if (!GlCaps.mrt || !GlCaps.floatTex)
            return Quality.LOW;
        // Without a stencil buffer auras lose the body mask and outlines use the inverted hull: keep the
        // glow, cut the distance.
        if (!GlCaps.stencil)
            return Quality.MEDIUM;
        return Quality.HIGH;
    }

    /** Recomputes the values the renderer reads. Call after loading the config, after init and on every change. */
    public static void apply() {
        Quality chosen = Quality.parse(ConfigDBCClient.QualityPreset);
        Quality q = chosen != null ? chosen : external != null ? external : auto();
        effective = q;

        ConfigDBCClient.EnableBloom = toggle(ConfigDBCClient.BloomSetting, q.bloom);
        ConfigDBCClient.EnableOutlines = toggle(ConfigDBCClient.OutlinesSetting, q.outlines);
        ConfigDBCClient.ModelDetailMaxDistance = number(ConfigDBCClient.ModelDetailMaxDistanceSetting, q.modelDetail);
        ConfigDBCClient.AuraMaxDistance = number(ConfigDBCClient.AuraMaxDistanceSetting, q.aura);
        ConfigDBCClient.OutlineMaxDistance = number(ConfigDBCClient.OutlineMaxDistanceSetting, q.outline);
        ConfigDBCClient.AuraParticleDensity = Math.min(100, number(ConfigDBCClient.AuraParticleDensitySetting, q.particles));
        ConfigDBCClient.BloomLowResolution = ConfigDBCClient.BloomLowResolutionSetting || q.bloomLowRes;
        ConfigDBCClient.BloomMaxLevels = number(ConfigDBCClient.BloomMaxLevelsSetting, q.bloomLevels);
    }

    private static boolean toggle(String setting, boolean preset) {
        if (ON.equalsIgnoreCase(setting))
            return true;
        if (OFF.equalsIgnoreCase(setting))
            return false;
        return preset;
    }

    private static int number(int setting, int preset) {
        return setting < 0 ? preset : setting;
    }
}
