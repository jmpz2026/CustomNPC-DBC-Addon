package kamkeel.npcdbc.api.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.npcdbc.client.shader.PostProcessing;
import kamkeel.npcdbc.client.shader.ShaderHelper;
import kamkeel.npcdbc.config.ConfigDBCClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Stable entry point for other mods that want their own geometry to glow with NpcDbc's bloom
 * (the same pass auras and outlines use). Meant to be called by reflection from mods that do not
 * depend on NpcDbc at compile time, so the signatures here do not change; add, never rename.
 *
 * <pre>
 *   if (BloomApi.begin()) {
 *       // draw what should glow (additive is the usual choice)
 *       BloomApi.end();
 *   }
 * </pre>
 *
 * Call it from the world pass (e.g. {@code RenderWorldLastEvent}): NpcDbc composites the bloom
 * later in the same frame, before the hand. What is drawn between begin and end lands both in
 * the scene and in the bloom buffer, so nothing has to be drawn twice.
 *
 * <p>{@code begin()} returns false, and draws nothing differently, when bloom is off in the
 * config, shaders are unsupported, or another pipeline (an Iris shaderpack) owns the target. In
 * that case draw normally and do not call {@code end()}.
 */
@SideOnly(Side.CLIENT)
public final class BloomApi {

    private BloomApi() {}

    /** Whether bloom would be applied right now (config, shader support, buffers ready). */
    public static boolean isAvailable() {
        return PostProcessing.bloomSupported && PostProcessing.hasInitialized
                && ConfigDBCClient.EnableBloom && ShaderHelper.shadersEnabled();
    }

    /**
     * Start drawing into the bloom buffer. Returns true only if the bloom framebuffer is bound;
     * then {@link #end()} must follow.
     */
    public static boolean begin() {
        if (!isAvailable()) return false;
        PostProcessing.startBlooming(false);
        return GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING) == PostProcessing.MAIN_BLOOM_BUFFER;
    }

    /** Back to the framebuffer that was bound before {@link #begin()}. */
    public static void end() {
        PostProcessing.endBlooming();
    }
}
