package kamkeel.npcdbc.client.render;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Turning the lightmap off for an aura, outline or ki blade and back on afterwards assumed it
 * was on to begin with. It is not always: the middle render pass and vanilla's pass 1 run after
 * EntityRenderer has already turned it off. Turning it back on there leaked it past the world,
 * and in third person (no hand pass to reset it) the next frame and the HUD were drawn with the
 * lightmap of the last NPC on screen: the whole view went dark or flickered. Save the state
 * before turning it off and put back exactly that.
 */
@SideOnly(Side.CLIENT)
public final class LightmapState {

    private LightmapState() {}

    /** Whether the lightmap texture unit currently has texturing enabled. */
    public static boolean isOn() {
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        boolean on = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(active);
        return on;
    }

    /** Back to the state returned by {@link #isOn()}. */
    public static void restore(boolean on) {
        if (on)
            Minecraft.getMinecraft().entityRenderer.enableLightmap(0);
        else
            Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
    }
}
