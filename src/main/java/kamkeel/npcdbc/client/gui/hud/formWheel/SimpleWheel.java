package kamkeel.npcdbc.client.gui.hud.formWheel;

import kamkeel.npcdbc.CommonProxy;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Decides whether the form wheel opens in its simple mode: no open/close animation, no background blur, a fixed
 * dark overlay. The animated wheel depends on steady clocks and on post-processing framebuffers, and on Android, on
 * desktop Linux and on weak GPUs either one has left the screen black after opening it. The simple wheel draws at
 * full scale from the first frame and closes at once, so nothing it shows depends on time.
 *
 * <p>Platform and GPU are checked once. The DbrCore performance mode is read on every open, because the player can
 * switch it in game.
 */
public final class SimpleWheel {

    /** GL_RENDERER fragments of GPUs that get the simple wheel. Matched lowercase. */
    private static final String[] WEAK_GPUS = {"mali", "adreno", "powervr", "llvmpipe", "softpipe"};

    private static Boolean platformOrGpu;

    private SimpleWheel() {}

    /** Call on the render thread with a GL context current: the first call reads GL_RENDERER. */
    public static boolean active() {
        if (platformOrGpu == null) {
            platformOrGpu = detectPlatformOrGpu();
        }
        return platformOrGpu || dbrLightMode();
    }

    private static boolean detectPlatformOrGpu() {
        final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        final String osVersion = System.getProperty("os.version", "").toLowerCase(Locale.ROOT);
        String renderer = "";
        try {
            final String r = GL11.glGetString(GL11.GL_RENDERER);
            if (r != null)
                renderer = r;
        } catch (Throwable ignored) {
        }

        final String reason;
        if (osVersion.contains("android"))
            reason = "Android";
        else if (osName.contains("linux"))
            reason = "Linux";
        else if (isWeakGpu(renderer.toLowerCase(Locale.ROOT)))
            reason = "GPU " + renderer;
        else
            reason = null;

        if (reason != null)
            CommonProxy.LOGGER.info("Form wheel: simple mode ({})", reason);
        return reason != null;
    }

    static boolean isWeakGpu(String renderer) {
        for (String gpu : WEAK_GPUS) {
            if (renderer.contains(gpu))
                return true;
        }
        // Intel HD/UHD and the older GMA/Express chips. Iris Xe and Arc are left out on purpose.
        return renderer.contains("intel")
            && (renderer.contains("hd graphics") || renderer.contains("uhd graphics")
            || renderer.contains("gma") || renderer.contains("express"));
    }

    /** {@code preferencia=ligero} in DbrCore's config/dbrcore/rendimiento.cfg. Read as a file: no DbrCore dependency. */
    private static boolean dbrLightMode() {
        final File cfg = new File(Minecraft.getMinecraft().mcDataDir, "config/dbrcore/rendimiento.cfg");
        if (!cfg.isFile())
            return false;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(cfg), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("S:preferencia="))
                    return "ligero".equalsIgnoreCase(line.substring("S:preferencia=".length()).trim());
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
