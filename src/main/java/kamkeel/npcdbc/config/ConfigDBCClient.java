package kamkeel.npcdbc.config;

import kamkeel.npcdbc.client.ClientProxy;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

public class ConfigDBCClient {
    public static Configuration config;

    public final static String GENERAL = "General";

    public static Property HideInfoMessageProperty;
    public static boolean HideInfoMessage = false;

    public final static String GUI = "Gui";

    public static boolean EnableDebugStatSheetSwitching = false;

    public static Property EnhancedGuiProperty;
    public static boolean EnhancedGui = true;
    public static Property DarkModeProperty;
    public static boolean DarkMode = true;

    public static Property AdvancedGuiModeProperty;
    public static boolean AdvancedGui = false;

    public final static String RENDERING = "Rendering";

    public static Property EnableHDTexturesProperty;
    public static boolean EnableHDTextures = false;

    public static Property RevampAuraProperty;
    public static boolean RevampAura = false;

    public static Property EnableOutlinesProperty;
    public static boolean EnableOutlines = true;
    public static Property OutlineMaxDistanceProperty;
    public static int OutlineMaxDistance = 0;
    public static Property EnableShadersProperty;
    public static boolean EnableShaders = true;
    public static Property EnableBloomProperty;
    public static boolean EnableBloom = true;
    public static Property EnableAurasProperty;
    public static boolean EnableAuras = true;
    public static Property EnableCustomParticlesProperty;
    public static boolean EnableCustomParticles = true;
    public static Property CustomParticleMaxCountProperty;
    public static int CustomParticleMaxCount = 0;
    public static Property AuraParticlePercentProperty;
    public static int AuraParticlePercent = 100;

    public static Property LowSpecModeProperty;
    public static boolean LowSpecMode = true;

    // Quality tunables (only relevant when the feature is on, i.e. Low Spec Mode off).
    public static Property BloomResShiftProperty;
    public static int BloomResShift = 1;
    public static Property MaxBloomLevelsProperty;
    public static int MaxBloomLevels = 6;
    public static Property AuraMaxLayersProperty;
    public static int AuraMaxLayers = 5;
    public static Property AuraLayerStepProperty;
    public static float AuraLayerStep = 0.05f;

    // Distance-based LOD (0 = off). Cuts draw calls for far/crowded entities.
    public static Property ModelDetailMaxDistanceProperty;
    public static int ModelDetailMaxDistance = 0;
    public static Property AuraMaxDistanceProperty;
    public static int AuraMaxDistance = 0;

    // --- Low-spec gates ---------------------------------------------------
    // When LowSpecMode is on, the heavy visual stack is forced off regardless
    // of the individual toggles. Turn LowSpecMode off to let the per-feature
    // toggles (and their vanilla defaults) take over.
    public static boolean bloomEnabled() {
        return !LowSpecMode && EnableBloom;
    }

    public static boolean outlinesEnabled() {
        return !LowSpecMode && EnableOutlines;
    }

    public static boolean aurasEnabled() {
        return !LowSpecMode && EnableAuras;
    }

    public static boolean particlesEnabled() {
        return !LowSpecMode && EnableCustomParticles;
    }

    /** True if another addon particle can be queued given the current per-entity count. */
    public static boolean particleQueueHasRoom(int currentSize) {
        return CustomParticleMaxCount <= 0 || currentSize < CustomParticleMaxCount;
    }

    /**
     * Effective aura-particle density percent. Low Spec Mode caps it at 25% so the
     * fork spawns far fewer EntityCusPar (big RAM/CPU/GC win) while keeping the look.
     */
    public static int auraParticlePercent() {
        return LowSpecMode ? Math.min(AuraParticlePercent, 25) : AuraParticlePercent;
    }

    public static Property FirstPerson3DAuraOpacityProperty;
    public static int FirstPerson3DAuraOpacity = 100;

    public static Property AlternateSelectionWheelTextureProperty;
    public static boolean AlteranteSelectionWheelTexture = true;

    public static void init(File configFile) {
        config = new Configuration(configFile);

        try {
            config.load();

            // General
            HideInfoMessageProperty = config.get(GENERAL, "Hide Info Messages", false, "Hides Change Form and other various transformation messages in game");
            HideInfoMessage = HideInfoMessageProperty.getBoolean(false);

            // GUI
            EnableDebugStatSheetSwitching = config.get(GUI, "Enable old stat sheet toggle", false, "DEBUG ONLY\n" +
                "This for finding inconsistencies between the GUIs, nothing else as the old GUI is deprecated due to: \n" +
                " >Incomplete custom form support.\n" +
                " >Lacks DBC-related fixes in the GUI (Ki protection, passive protection shenanigans, etc.)\n" +
                "\n" +
                "Please refrain from using the old GUI, as the replacement aims to be completely backwards compatible.\n" +
                "In the case you found a bug with it, please report it.\n" +
                "\n" +
                "Enables toggling between old DBC GUI and Addon replacements. ").getBoolean(false);
            EnhancedGuiProperty = config.get(GUI, "Enable Enhanced Gui", true, "Uses DBC Addons GUI for Coloring and Manipulation\n\nINFO: If DebugStatSheet switching is off, you cannot use old DBC GUI");
            EnhancedGui = EnhancedGuiProperty.getBoolean(true);
            DarkModeProperty = config.get(GUI, "Dark Mode", true, "Uses Dark Mode GUI in Enhanced Menu");
            DarkMode = DarkModeProperty.getBoolean(true);
            AdvancedGuiModeProperty = config.get(GUI, "Advanced GUI", false, "Shows Advanced Status Effects and Calculations in Menu");
            AdvancedGui = AdvancedGuiModeProperty.getBoolean(false);
            AlternateSelectionWheelTextureProperty = config.get(GUI, "Use Alternate Wheel GUI Texture", false, "Uses alternate texture for Wheel GUIs");
            AlteranteSelectionWheelTexture = AlternateSelectionWheelTextureProperty.getBoolean(false);

            // Rendering
            RevampAuraProperty = config.get(RENDERING, "Revamp Aura", false, "Renders with the new DBC Addon style of auras");
            RevampAura = RevampAuraProperty.getBoolean(false);

            EnableHDTexturesProperty = config.get(RENDERING, "Enable HD Textures", false, "Uses internal DBC Addon HD Textures");
            EnableHDTextures = EnableHDTexturesProperty.getBoolean(false);

            EnableOutlinesProperty = config.get(RENDERING, "Enable Outlines", true, "Enables outlines for players and NPCs");
            EnableOutlines = EnableOutlinesProperty.getBoolean(true);

            OutlineMaxDistanceProperty = config.get(RENDERING, "Outline Max Distance", 0, "Max distance (in blocks) at which outlines are rendered." + "\nOutlines are a costly extra render pass and are barely visible far away." + "\n0 = no limit (always render, default behavior)." + "\n(Min: 0)");
            OutlineMaxDistance = Math.max(0, OutlineMaxDistanceProperty.getInt(0));

            EnableShadersProperty = config.get(RENDERING, "Enable Shaders", true, "Enables the use of shaders when rendering");
            EnableShaders = EnableShadersProperty.getBoolean(true);

            EnableBloomProperty = config.get(RENDERING, "Enable Bloom", true, "Enables the bloom effect for player outlines and auras");
            EnableBloom = EnableBloomProperty.getBoolean(true);

            EnableAurasProperty = config.get(RENDERING, "Enable Auras", true, "Enables addon aura rendering (does not affect base DBC auras)");
            EnableAuras = EnableAurasProperty.getBoolean(true);

            EnableCustomParticlesProperty = config.get(RENDERING, "Enable Custom Particles", true, "Enables addon custom particle rendering");
            EnableCustomParticles = EnableCustomParticlesProperty.getBoolean(true);

            CustomParticleMaxCountProperty = config.get(RENDERING, "Custom Particle Max Count", 0, "Per-entity cap on queued addon particles. Extra particles fall back to base DBC rendering." + "\n0 = no cap (default). Lower this on weak GPUs instead of disabling particles entirely." + "\n(Min: 0)");
            CustomParticleMaxCount = Math.max(0, CustomParticleMaxCountProperty.getInt(0));

            AuraParticlePercentProperty = config.get(RENDERING, "Aura Particle Density Percent", 100, "Percentage of aura particles (EntityCusPar) actually spawned into the world." + "\nFewer particles = big RAM/CPU/GC savings since each is a real ticking entity." + "\nLow Spec Mode caps this at 25%. 0 = none, 100 = full (vanilla)." + "\n(Min: 0, Max: 100)");
            AuraParticlePercent = Math.max(0, Math.min(100, AuraParticlePercentProperty.getInt(100)));

            LowSpecModeProperty = config.get(RENDERING, "Low Spec Mode", true, "Master switch for ultra low-end PCs." + "\nWhen ON, the heavy visual stack (bloom, outlines, addon auras, custom particles)" + "\nis forced OFF regardless of the individual toggles above." + "\nTurn OFF to let the per-feature toggles take over." + "\nDefault ON for this performance-focused build.");
            LowSpecMode = LowSpecModeProperty.getBoolean(true);

            BloomResShiftProperty = config.get(RENDERING, "Bloom Resolution Shift", 1, "Extra downscale of the whole bloom mip chain. 0 = half-res (vanilla look), 1 = quarter-res (~4x fewer bloom pixels, default). Higher = cheaper/softer." + "\n(Min: 0, Max: 4)");
            BloomResShift = Math.max(0, Math.min(4, BloomResShiftProperty.getInt(1)));
            MaxBloomLevelsProperty = config.get(RENDERING, "Max Bloom Levels", 6, "Hard cap on bloom mip levels processed. Fewer = cheaper, smaller glow radius." + "\n(Min: 1, Max: 8)");
            MaxBloomLevels = Math.max(1, Math.min(8, MaxBloomLevelsProperty.getInt(6)));
            AuraMaxLayersProperty = config.get(RENDERING, "Aura Max Layers", 5, "Aura layer count. Fewer = fewer model renders = more FPS, thinner aura. Vanilla = 5." + "\n(Min: 1, Max: 5)");
            AuraMaxLayers = Math.max(1, Math.min(5, AuraMaxLayersProperty.getInt(5)));
            AuraLayerStepProperty = config.get(RENDERING, "Aura Layer Step", 0.05, "Aura inner-loop step. Bigger = fewer iterations = more FPS. Vanilla = 0.05." + "\n(Min: 0.05, Max: 1.0)");
            AuraLayerStep = (float) Math.max(0.05, Math.min(1.0, AuraLayerStepProperty.getDouble(0.05)));

            ModelDetailMaxDistanceProperty = config.get(RENDERING, "Model Detail Max Distance", 0, "Max distance (blocks) at which NPC face detail (eyes/nose/mouth/brows) is drawn." + "\nThe face is several extra draw calls and is invisible far away. Skipping it cuts" + "\ndraw calls for distant/crowded NPCs (applies even in Low Spec Mode)." + "\n0 = always draw (default). Suggested for low-end: 24." + "\n(Min: 0)");
            ModelDetailMaxDistance = Math.max(0, ModelDetailMaxDistanceProperty.getInt(0));
            AuraMaxDistanceProperty = config.get(RENDERING, "Aura LOD Distance", 0, "Distance (blocks) past which auras render with half the layers and a coarser step" + "\n(~4x fewer model renders). The glow is nearly identical far away." + "\n0 = full detail at any distance (default). Suggested for low-end: 32." + "\n(Min: 0)");
            AuraMaxDistance = Math.max(0, AuraMaxDistanceProperty.getInt(0));

            FirstPerson3DAuraOpacityProperty = config.get(RENDERING, "First person 3D Aura Opacity", 100, "The opacity of the first person 3D Aura." + "\nModifying this makes it so auras on other players render normally without blinding you" + "\n(Min: 0, Max: 100)");
            FirstPerson3DAuraOpacity = Math.max(Math.min(100, FirstPerson3DAuraOpacityProperty.getInt(100)), 0);
        } catch (Exception e) {
            ClientProxy.LOGGER.error("Error loading client configuration: " + e.getMessage());
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
}
