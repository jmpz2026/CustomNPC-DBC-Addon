package kamkeel.npcdbc.client.shader;

import kamkeel.npcdbc.CommonProxy;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

/**
 * What the GPU and the game settings actually allow, read once per {@link PostProcessing#init}.
 * Old iGPUs (Intel HD 2000-4000 with 2015 drivers, Mesa software, GL4ES) miss some of these;
 * every effect checks here first and degrades instead of drawing into a broken target.
 *
 * <p>For tests on a strong PC, each capability can be forced off with a system property:
 * {@code -Dnpcdbc.caps.nostencil}, {@code .nofloat}, {@code .nomrt}, {@code .nofbo}, {@code .noshaders}.
 */
public final class GlCaps {

    /** Framebuffer objects exist and are turned on in Video Settings. */
    public static boolean fbo;
    /** The main framebuffer has a stencil buffer. */
    public static boolean stencil;
    /** RGBA16F render targets; without them the bloom chain uses RGBA8. */
    public static boolean floatTex;
    /** At least 3 draw buffers (the bloom framebuffer writes attachments 0 and 2). */
    public static boolean mrt;
    /** The bloom shaders compiled and linked. */
    public static boolean shaders;

    private static String lastLine;

    private GlCaps() {}

    static void refresh(boolean stencilDetected) {
        ContextCapabilities caps = GLContext.getCapabilities();
        fbo = OpenGlHelper.isFramebufferEnabled() && !forcedOff("nofbo");
        stencil = stencilDetected && !forcedOff("nostencil");
        floatTex = (caps.OpenGL30 || caps.GL_ARB_texture_float) && !forcedOff("nofloat");
        mrt = caps.OpenGL20 && GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS) >= 3 && !forcedOff("nomrt");
        shaders = ShaderHelper.shadersEnabled() && ShaderHelper.bloomShadersLinked() && !forcedOff("noshaders");

        String line = "npcdbc GL caps: fbo=" + fbo + " stencil=" + stencil + " float=" + floatTex
            + " mrt=" + mrt + " shaders=" + shaders;
        // init runs again on every resize: log only when something changed.
        if (!line.equals(lastLine))
            CommonProxy.LOGGER.info(line);
        lastLine = line;
    }

    /** Whether the bloom pipeline can run at all on this GPU. */
    public static boolean bloomPipeline() {
        return fbo && mrt && shaders;
    }

    private static boolean forcedOff(String name) {
        return Boolean.getBoolean("npcdbc.caps." + name);
    }
}
