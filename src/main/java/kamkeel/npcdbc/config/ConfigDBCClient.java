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

    public static Property EnableShadersProperty;
    public static boolean EnableShaders = true;

    public static Property FirstPerson3DAuraOpacityProperty;
    public static int FirstPerson3DAuraOpacity = 100;

    // Effective rendering values: what the renderer reads. RenderSettings.apply() fills them from the
    // quality preset and the manual settings below; nothing saves them to disk.
    public static boolean EnableBloom = true;
    public static boolean EnableOutlines = true;
    public static int ModelDetailMaxDistance = 0;
    public static int AuraMaxDistance = 0;
    public static int AuraParticleDensity = 100;
    public static int OutlineMaxDistance = 0;
    public static boolean BloomLowResolution = false;
    public static int BloomMaxLevels = 0;

    // What the player wrote in client.cfg. "preset" / -1 = follow the Graphics Quality preset.
    public static Property QualityPresetProperty, BloomSettingProperty, OutlinesSettingProperty;
    public static String QualityPreset = "AUTO";
    public static String BloomSetting = "preset", OutlinesSetting = "preset";
    public static int ModelDetailMaxDistanceSetting = -1, AuraMaxDistanceSetting = -1, OutlineMaxDistanceSetting = -1;
    public static int AuraParticleDensitySetting = -1, BloomMaxLevelsSetting = -1;
    public static boolean BloomLowResolutionSetting = false;

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

            EnableShadersProperty = config.get(RENDERING, "Enable Shaders", true, "Enables the use of shaders when rendering");
            EnableShaders = EnableShadersProperty.getBoolean(true);

            FirstPerson3DAuraOpacityProperty = config.get(RENDERING, "First person 3D Aura Opacity", 100, "The opacity of the first person 3D Aura." + "\nModifying this makes it so auras on other players render normally without blinding you" + "\n(Min: 0, Max: 100)");
            FirstPerson3DAuraOpacity = Math.max(Math.min(100, FirstPerson3DAuraOpacityProperty.getInt(100)), 0);

            loadQuality();
        } catch (Exception e) {
            ClientProxy.LOGGER.error("Error loading client configuration: " + e.getMessage());
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    private static final String[] QUALITY_VALUES = {"AUTO", "HIGH", "MEDIUM", "LOW", "VERY_LOW"};
    private static final String[] TOGGLE_VALUES = {"preset", "on", "off"};
    private static final String PRESET_NOTE = "\n-1 = follow the Graphics Quality preset";

    private static void loadQuality() {
        // Version 1: the on/off toggles became preset/on/off and the performance numbers gained -1
        // (follow the preset). Older files wrote the old defaults; those mean "follow the preset" now.
        boolean migrate = !config.getCategory(RENDERING).containsKey("Quality Settings Version");
        String oldBloom = migrateToggle("Enable Bloom"), oldOutlines = migrateToggle("Enable Outlines");

        QualityPresetProperty = config.get(RENDERING, "Graphics Quality", "AUTO",
            "AUTO picks from what the GPU supports. Another mod (DbrOptimizer) may lower AUTO; a fixed choice here always wins."
                + "\nHIGH = original look. MEDIUM, LOW and VERY_LOW cut glow, outlines and detail at distance.",
            QUALITY_VALUES);
        QualityPreset = QualityPresetProperty.getString().toUpperCase();

        BloomSettingProperty = config.get(RENDERING, "Bloom", oldBloom != null ? oldBloom : "preset",
            "Glow around outlines and auras. preset = what Graphics Quality says.", TOGGLE_VALUES);
        BloomSetting = BloomSettingProperty.getString();
        OutlinesSettingProperty = config.get(RENDERING, "Outlines", oldOutlines != null ? oldOutlines : "preset",
            "Outlines for players and NPCs. preset = what Graphics Quality says.", TOGGLE_VALUES);
        OutlinesSetting = OutlinesSettingProperty.getString();

        ModelDetailMaxDistanceSetting = presetInt("Model Detail Max Distance", 0, migrate,
            "NPC faces (eyes, nose, mouth, brows) are not drawn beyond this many blocks.\n0 = always drawn");
        AuraMaxDistanceSetting = presetInt("Aura Max Distance", 0, migrate,
            "Beyond this many blocks, addon auras are drawn with half the passes and a matching opacity.\n0 = full detail at any distance");
        OutlineMaxDistanceSetting = presetInt("Outline Max Distance", 0, migrate,
            "Outlines are not drawn beyond this many blocks.\n0 = always drawn");
        BloomMaxLevelsSetting = presetInt("Bloom Max Levels", 0, migrate,
            "Caps the bloom mip levels. Fewer levels = cheaper and a shorter glow.\n0 = all levels");
        AuraParticleDensitySetting = Math.min(100, presetInt("Aura Particle Density", 100, migrate,
            "Percentage of custom form aura particles that are spawned. Particles in GUIs are never cut.\n(Min: 0, Max: 100)"));
        BloomLowResolutionSetting = config.get(RENDERING, "Bloom Low Resolution", false,
            "Starts the bloom at quarter resolution instead of half. Much cheaper; the glow gets a bit softer."
                + "\nfalse = what Graphics Quality says").getBoolean(false);

        config.get(RENDERING, "Quality Settings Version", 1, "Do not edit.").set(1);
    }

    /** Reads and removes an old true/false key: false becomes a manual "off", true follows the preset. */
    private static String migrateToggle(String oldKey) {
        if (!config.getCategory(RENDERING).containsKey(oldKey))
            return null;
        boolean on = config.getCategory(RENDERING).get(oldKey).getBoolean(true);
        config.getCategory(RENDERING).remove(oldKey);
        return on ? "preset" : "off";
    }

    private static int presetInt(String key, int oldDefault, boolean migrate, String comment) {
        Property property = config.get(RENDERING, key, -1, comment + PRESET_NOTE);
        if (migrate && property.getInt(-1) == oldDefault)
            property.set(-1);
        return Math.max(-1, property.getInt(-1));
    }

    /** Saves a value picked in the in-game menu (a preset name, or "preset" / "on" / "off"). */
    public static void saveToggle(Property property, String value) {
        property.set(value);
        config.save();
    }
}
