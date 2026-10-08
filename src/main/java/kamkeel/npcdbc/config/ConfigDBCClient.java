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
    public static Property EnableShadersProperty;
    public static boolean EnableShaders = true;
    public static Property EnableBloomProperty;
    public static boolean EnableBloom = true;

    public static Property FirstPerson3DAuraOpacityProperty;
    public static int FirstPerson3DAuraOpacity = 100;

    // Performance. The defaults keep the original look; DbrMod's light mode sets these fields in memory.
    public static int ModelDetailMaxDistance = 0;
    public static int AuraMaxDistance = 0;
    public static int AuraParticleDensity = 100;
    public static int OutlineMaxDistance = 0;
    public static boolean BloomLowResolution = false;
    public static int BloomMaxLevels = 0;

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

            EnableShadersProperty = config.get(RENDERING, "Enable Shaders", true, "Enables the use of shaders when rendering");
            EnableShaders = EnableShadersProperty.getBoolean(true);

            EnableBloomProperty = config.get(RENDERING, "Enable Bloom", true, "Enables the bloom effect for player outlines and auras");
            EnableBloom = EnableBloomProperty.getBoolean(true);

            FirstPerson3DAuraOpacityProperty = config.get(RENDERING, "First person 3D Aura Opacity", 100, "The opacity of the first person 3D Aura." + "\nModifying this makes it so auras on other players render normally without blinding you" + "\n(Min: 0, Max: 100)");
            FirstPerson3DAuraOpacity = Math.max(Math.min(100, FirstPerson3DAuraOpacityProperty.getInt(100)), 0);

            ModelDetailMaxDistance = Math.max(0, config.get(RENDERING, "Model Detail Max Distance", 0, "NPC faces (eyes, nose, mouth, brows) are not drawn beyond this many blocks." + "\n0 = always drawn").getInt(0));
            AuraMaxDistance = Math.max(0, config.get(RENDERING, "Aura Max Distance", 0, "Beyond this many blocks, addon auras are drawn with half the passes and a matching opacity." + "\n0 = full detail at any distance").getInt(0));
            OutlineMaxDistance = Math.max(0, config.get(RENDERING, "Outline Max Distance", 0, "Outlines are not drawn beyond this many blocks." + "\n0 = always drawn").getInt(0));
            BloomLowResolution = config.get(RENDERING, "Bloom Low Resolution", false, "Starts the bloom at quarter resolution instead of half. Much cheaper; the glow gets a bit softer.").getBoolean(false);
            BloomMaxLevels = Math.max(0, config.get(RENDERING, "Bloom Max Levels", 0, "Caps the bloom mip levels. Fewer levels = cheaper and a shorter glow." + "\n0 = all levels").getInt(0));
            AuraParticleDensity = Math.max(0, Math.min(100, config.get(RENDERING, "Aura Particle Density", 100, "Percentage of custom form aura particles that are spawned. Particles in GUIs are never cut." + "\n(Min: 0, Max: 100)").getInt(100)));
        } catch (Exception e) {
            ClientProxy.LOGGER.error("Error loading client configuration: " + e.getMessage());
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
}
