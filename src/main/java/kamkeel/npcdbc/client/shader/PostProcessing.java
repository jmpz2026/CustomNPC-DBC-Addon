package kamkeel.npcdbc.client.shader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.npcdbc.CommonProxy;
import kamkeel.npcdbc.client.ClientConstants;
import kamkeel.npcdbc.client.gui.hud.formWheel.HUDFormWheel;
import kamkeel.npcdbc.config.ConfigDBCClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static kamkeel.npcdbc.client.shader.ShaderHelper.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

@SideOnly(Side.CLIENT)
public class PostProcessing {

    public static Framebuffer MAIN;
    public static int MAIN_BLOOM_BUFFER, MAIN_BLOOM_TEXTURE, DEPTH_TEXTURE;
    public static int MISC_POST_PROCESSING_BUFFER, BLUR_TEXTURE;
    public static int blankTexture;

    public static int BLOOM_BUFFERS_LENGTH = 10;
    public static int[] bloomBuffers = new int[BLOOM_BUFFERS_LENGTH];
    public static int[] bloomTextures = new int[bloomBuffers.length];
    public static int[] bloomTextures2 = new int[bloomBuffers.length];

    public static int auraBuffer;
    public static int[] auraTextures = new int[3];

    public static boolean processBloom;
    public static boolean bloomSupported = true; // ← new flag

    // Copy of the main framebuffer the bloom combine samples from: reading MAIN.framebufferTexture while
    // MAIN is the draw target is a feedback loop, undefined behavior that some drivers resolve with a stale
    // read that wipes whatever was drawn since (the Y form wheel flicker).
    public static int SCENE_COPY_TEXTURE;
    private static int sceneCopyWidth, sceneCopyHeight;

    // Whether the main framebuffer has a stencil buffer. Outlines and auras mask the body with it; without
    // one the outline falls back to an inverted hull instead of painting the whole silhouette.
    public static boolean stencilAvailable = true;

    // OptiFine keeps its own path untouched: its shaders bind their FBO for the whole world pass.
    private static final boolean OPTIFINE = classExists("optifine.OptiFineForgeTweaker");

    public static FloatBuffer DEFAULT_MODELVIEW = BufferUtils.createFloatBuffer(16);
    public static FloatBuffer DEFAULT_PROJECTION = BufferUtils.createFloatBuffer(16);
    public static Minecraft mc = Minecraft.getMinecraft();
    public static int PREVIOUS_BUFFER;
    public static int VIEWPORT_WIDTH, VIEWPORT_HEIGHT;

    public static boolean hasInitialized;

    private static boolean isScissorEnabled;

    public static void startBlooming(boolean clearBloomBuffer) {
        if (!bloomSupported || !ConfigDBCClient.EnableBloom || !ShaderHelper.shadersEnabled())
            return;

        int currentBuffer = glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        // Another pipeline (an Iris shaderpack) owns the world render target: whatever lands in MAIN is
        // overwritten by its final pass. Draw the effect straight into its target, without bloom.
        if (!OPTIFINE && !ClientConstants.renderingGUI && MAIN != null && currentBuffer != MAIN.framebufferObject)
            return;

        PREVIOUS_BUFFER = currentBuffer;
        glBindFramebuffer(GL_FRAMEBUFFER, MAIN_BLOOM_BUFFER);
        if (clearBloomBuffer) {
            drawToBuffers(2);
            glClearColor(0, 0, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT);
        }

        drawToBuffers(0, 2);
        processBloom = true;
    }

    public static void endBlooming() {
        if (processBloom && bloomSupported) {
            glBindFramebuffer(GL_FRAMEBUFFER, PREVIOUS_BUFFER);
        }
    }

    public static void postProcess() {
        // when bloomSupported is false, skip bloom entirely
        if (bloomSupported) {
            bloom(1.5f, false);
        }

        if (bloomSupported && ShaderHelper.shadersEnabled() &&
            mc.currentScreen instanceof HUDFormWheel && HUDFormWheel.BLUR_ENABLED
            // The first frame after opening still has intensity 0; the shader divides by it (black frame on Mesa).
            && HUDFormWheel.BLUR_INTENSITY > 0.01f) {
            // Runs right before the HUD and the GUI: hand them back the state they had.
            GLStateSnapshot snapshot = GLStateSnapshot.capture();
            Framebuffer buff = getMainBuffer();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, mc.displayWidth, mc.displayHeight, 0.0D, 0, 1);

            glBindFramebuffer(GL_FRAMEBUFFER, MISC_POST_PROCESSING_BUFFER);
            drawToBuffers(0);
            disableGLState();

            blurVertical(buff.framebufferTexture, HUDFormWheel.BLUR_INTENSITY, 0, 0, mc.displayWidth, mc.displayHeight);

            buff.bindFramebuffer(false);
            disableGLState();
            blurHorizontal(BLUR_TEXTURE, HUDFormWheel.BLUR_INTENSITY, 0, 0, mc.displayWidth, mc.displayHeight);
            releaseShader();

            snapshot.restore();
        }

        if (isScissorEnabled)
            GL11.glEnable(GL_SCISSOR_TEST);
    }

    public static void bloom(float lightExposure, boolean resetGLState) {
        if (!bloomSupported)
            return;

        isScissorEnabled = GL11.glIsEnabled(GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (!processBloom)
            return;

        // The postProcess path (resetGLState == false) runs right before the HUD, which
        // inherits whatever GL state is left behind. Snapshot it so the HUD sees exactly
        // what it would see with bloom off (fixes black chat text on strict drivers like Mesa).
        GLStateSnapshot snapshot = resetGLState ? null : GLStateSnapshot.capture();
        int previousBuffer = glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        updateViewportDimensions();
        int width = VIEWPORT_WIDTH, height = VIEWPORT_HEIGHT;
        FloatBuffer prevModelView = getModelView();
        FloatBuffer prevProjection = getProjection();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, width, height, 0.0D, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glColorMask(true, true, true, false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        mc.entityRenderer.disableLightmap(0);

        // Down Sampling in a mip chain
        glBindFramebuffer(GL_FRAMEBUFFER, bloomBuffers[0]);
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT);
        glViewport(0, 0, width >> 1, height >> 1);
        useShader(downsample13);
        renderQuad(MAIN_BLOOM_TEXTURE, 0, 0, width, height);
        // Separable blur ping-pongs through the second attachment instead of reading the texture it writes.
        drawToBuffers(2);
        blurVertical(bloomTextures[0], 2.5f, 0, 0, width, height);
        resetDrawBuffer();
        blurHorizontal(bloomTextures2[0], 2.5f, 0, 0, width, height);
        releaseShader();
        int downSamples = 0;
        for (int i = 0; i < bloomBuffers.length; i++) {
            // Stop at the last allocated mip: bloomBuffers[i + 1] == 0 would bind the default framebuffer
            if (i + 1 >= bloomBuffers.length || bloomBuffers[i] <= 0 || bloomBuffers[i + 1] <= 0)
                break;
            int mipWidth = width >> (i + 2), mipHeight = height >> (i + 2);
            glBindFramebuffer(GL_FRAMEBUFFER, bloomBuffers[i + 1]);
            glViewport(0, 0, mipWidth, mipHeight);
            useShader(downsample13);
            renderQuad(bloomTextures[i], 0, 0, width, height);
            downSamples = i + 1;
        }

        // Up sampling the mip chain
        for (int i = downSamples; i > 0; i--) {
            int lower = bloomTextures[i];
            int mipWidth = width >> (i), mipHeight = height >> (i);
            glBindFramebuffer(GL_FRAMEBUFFER, bloomBuffers[i - 1]);

            drawToBuffers(2);
            glViewport(0, 0, mipWidth, mipHeight);
            blurFilter(lower, 1f, 0, 0, width, height);
            resetDrawBuffer();
            int lowerUpscaled = bloomTextures2[i - 1];

            glEnable(GL_BLEND);
            glBlendFunc(GL_ONE, GL_ONE);
            renderQuad(lowerUpscaled, 0, 0, width, height);
            glDisable(GL_BLEND);
        }

        // Combine into default buffer
        MAIN.bindFramebuffer(false);
        glViewport(0, 0, width, height);
        int sceneTexture = copyScene(width, height);
        useShader(additiveCombine, () -> {
            uniformTexture("bloomTexture", 2, bloomTextures[0]);
            uniform1f("exposure", lightExposure);
        });
        renderQuad(sceneTexture, 0, 0, width, height);
        releaseShader();

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        if (resetGLState) {
            mc.entityRenderer.enableLightmap(0);
            glMatrixMode(GL11.GL_PROJECTION);
            glLoadMatrix(prevProjection);
            glMatrixMode(GL11.GL_MODELVIEW);
            glLoadMatrix(prevModelView);
            glEnable(GL_LIGHTING);
            glEnable(GL_ALPHA_TEST);
            glColorMask(true, true, true, true);
            if (!ClientConstants.renderingGUI && !ClientConstants.renderingArm)
                glEnable(GL_FOG);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, MAIN_BLOOM_BUFFER);
        drawToBuffers(2);
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT);

        if (OPTIFINE) {
            MAIN.bindFramebuffer(false);
        } else {
            glBindFramebuffer(GL_FRAMEBUFFER, previousBuffer);
            glViewport(0, 0, width, height);
        }
        processBloom = false;

        if (snapshot != null)
            snapshot.restore();
    }

    /**
     * Fixed-function state that bloom() touches and the in-game HUD relies on.
     */
    private static final class GLStateSnapshot {
        private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
        private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        private final FloatBuffer color = BufferUtils.createFloatBuffer(16);
        private final FloatBuffer clearColor = BufferUtils.createFloatBuffer(16);
        private final ByteBuffer colorMask = BufferUtils.createByteBuffer(16);
        private int matrixMode, framebuffer, program, activeTexture;
        private int blendSrcRGB, blendDstRGB, blendSrcAlpha, blendDstAlpha;
        private int texture0, texture2;
        private boolean blend, alphaTest, depthTest, depthMask, lighting, fog, texture2D, colorMaterial, lightmap;

        static GLStateSnapshot capture() {
            GLStateSnapshot s = new GLStateSnapshot();
            s.matrixMode = glGetInteger(GL_MATRIX_MODE);
            glGetFloat(GL_MODELVIEW_MATRIX, s.modelView);
            glGetFloat(GL_PROJECTION_MATRIX, s.projection);
            glGetFloat(GL_CURRENT_COLOR, s.color);
            glGetFloat(GL_COLOR_CLEAR_VALUE, s.clearColor);
            glGetBoolean(GL_COLOR_WRITEMASK, s.colorMask);
            s.framebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING);
            s.program = glGetInteger(GL20.GL_CURRENT_PROGRAM);
            s.blendSrcRGB = glGetInteger(GL14.GL_BLEND_SRC_RGB);
            s.blendDstRGB = glGetInteger(GL14.GL_BLEND_DST_RGB);
            s.blendSrcAlpha = glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            s.blendDstAlpha = glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            s.blend = glIsEnabled(GL_BLEND);
            s.alphaTest = glIsEnabled(GL_ALPHA_TEST);
            s.depthTest = glIsEnabled(GL_DEPTH_TEST);
            s.depthMask = glGetBoolean(GL_DEPTH_WRITEMASK);
            s.lighting = glIsEnabled(GL_LIGHTING);
            s.fog = glIsEnabled(GL_FOG);
            s.colorMaterial = glIsEnabled(GL_COLOR_MATERIAL);

            s.activeTexture = glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            s.texture2D = glIsEnabled(GL_TEXTURE_2D);
            s.texture0 = glGetInteger(GL_TEXTURE_BINDING_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            s.lightmap = glIsEnabled(GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE2);
            s.texture2 = glGetInteger(GL_TEXTURE_BINDING_2D);
            OpenGlHelper.setActiveTexture(s.activeTexture);
            return s;
        }

        void restore() {
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            ARBShaderObjects.glUseProgramObjectARB(program);

            glMatrixMode(GL_PROJECTION);
            glLoadMatrix(projection);
            glMatrixMode(GL_MODELVIEW);
            glLoadMatrix(modelView);
            glMatrixMode(matrixMode);

            glColor4f(color.get(0), color.get(1), color.get(2), color.get(3));
            glClearColor(clearColor.get(0), clearColor.get(1), clearColor.get(2), clearColor.get(3));
            glColorMask(colorMask.get(0) != 0, colorMask.get(1) != 0, colorMask.get(2) != 0, colorMask.get(3) != 0);
            OpenGlHelper.glBlendFunc(blendSrcRGB, blendDstRGB, blendSrcAlpha, blendDstAlpha);
            setState(GL_BLEND, blend);
            setState(GL_ALPHA_TEST, alphaTest);
            setState(GL_DEPTH_TEST, depthTest);
            glDepthMask(depthMask);
            setState(GL_LIGHTING, lighting);
            setState(GL_FOG, fog);
            setState(GL_COLOR_MATERIAL, colorMaterial);

            OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, texture2);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            setState(GL_TEXTURE_2D, lightmap);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            setState(GL_TEXTURE_2D, texture2D);
            glBindTexture(GL_TEXTURE_2D, texture0);
            OpenGlHelper.setActiveTexture(activeTexture);
        }

        private static void setState(int cap, boolean enabled) {
            if (enabled)
                glEnable(cap);
            else
                glDisable(cap);
        }
    }

    public static void captureSceneDepth() {
        Framebuffer buff = getMainBuffer();
        int width = mc.displayWidth, height = mc.displayHeight;
        ByteBuffer depthBuff = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuff);

        ByteBuffer rgbaBuffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int i = 0; i < width * height; i++) {
            float depth = depthBuff.getFloat(i * Float.BYTES);
            float depth2 = depth == 1 ? 0 : 1;
            rgbaBuffer.put((byte) (depth * 255));
            rgbaBuffer.put((byte) (depth * 255));
            rgbaBuffer.put((byte) (depth * 255));
            rgbaBuffer.put((byte) (depth == 1 ? 0 : 255));
        }
        rgbaBuffer.flip();

        GL11.glBindTexture(GL_TEXTURE_2D, DEPTH_TEXTURE);
        GL11.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgbaBuffer);
    }

    public static void disableGLState() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glColorMask(true, true, true, false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        mc.entityRenderer.disableLightmap(0);
    }

    public static void enableGLState() {
        mc.entityRenderer.enableLightmap(0);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glEnable(GL_BLEND);
        glEnable(GL_ALPHA_TEST);
        glEnable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_COLOR_MATERIAL);
        glColorMask(true, true, true, true);
    }

    public static void blurVertical(int textureID, float blurIntensity, float startX, float startY, float width, float height) {
        useShader(blur, () -> {
            uniformVec2("u_resolution", width - startX, height - startY);
            uniform1i("horizontal", 0);
            uniform1f("blurIntensity", blurIntensity);
        });
        renderQuad(textureID, startX, startY, width, height);
    }

    public static void blurHorizontal(int textureID, float blurIntensity, float startX, float startY, float width, float height) {
        useShader(blur, () -> {
            uniformVec2("u_resolution", width - startX, height - startY);
            uniform1i("horizontal", 1);
            uniform1f("blurIntensity", blurIntensity);
        });
        renderQuad(textureID, startX, startY, width, height);
    }

    public static void blurFilter(int textureID, float blurIntensity, float startX, float startY, float width, float height) {
        blurVertical(textureID, blurIntensity, startX, startY, width, height);
        blurHorizontal(textureID, blurIntensity, startX, startY, width, height);
        releaseShader();
    }

    public static void setupDepthAndStencil() {
        OpenGlHelper.func_153176_h(OpenGlHelper.field_153199_f, MAIN.depthBuffer);
        if (net.minecraftforge.client.MinecraftForgeClient.getStencilBits() == 0) {
            OpenGlHelper.func_153186_a(OpenGlHelper.field_153199_f, 33190,
                MAIN.framebufferTextureWidth, MAIN.framebufferTextureHeight);
            OpenGlHelper.func_153190_b(OpenGlHelper.field_153198_e,
                OpenGlHelper.field_153201_h, OpenGlHelper.field_153199_f, MAIN.depthBuffer);
        } else {
            OpenGlHelper.func_153186_a(OpenGlHelper.field_153199_f,
                org.lwjgl.opengl.EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT,
                MAIN.framebufferTextureWidth, MAIN.framebufferTextureHeight);
            OpenGlHelper.func_153190_b(OpenGlHelper.field_153198_e,
                org.lwjgl.opengl.EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                OpenGlHelper.field_153199_f, MAIN.depthBuffer);
            OpenGlHelper.func_153190_b(OpenGlHelper.field_153198_e,
                org.lwjgl.opengl.EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT,
                OpenGlHelper.field_153199_f, MAIN.depthBuffer);
        }
    }

    public static void init(int width, int height) {
        hasInitialized = true;
        bloomSupported = true;
        stencilAvailable = detectStencil();

        // Minimal check: if FBOs or shaders aren’t supported, disable bloom entirely
        if (!OpenGlHelper.framebufferSupported || !ShaderHelper.shadersEnabled()) {
            bloomSupported = false;
            return;
        }

        int previousBuffer = glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        MAIN = getMainBuffer();

        MISC_POST_PROCESSING_BUFFER = OpenGlHelper.func_153165_e();
        GL30.glBindFramebuffer(GL_FRAMEBUFFER, MISC_POST_PROCESSING_BUFFER);

        BLUR_TEXTURE = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, BLUR_TEXTURE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        OpenGlHelper.func_153188_a(GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, BLUR_TEXTURE, 0);

        MAIN_BLOOM_BUFFER = OpenGlHelper.func_153165_e();
        GL30.glBindFramebuffer(GL_FRAMEBUFFER, MAIN_BLOOM_BUFFER);

        OpenGlHelper.func_153188_a(GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, MAIN.framebufferTexture, 0);

        MAIN_BLOOM_TEXTURE = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, MAIN_BLOOM_TEXTURE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        OpenGlHelper.func_153188_a(GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, MAIN_BLOOM_TEXTURE, 0);
        setupDepthAndStencil();

        drawToBuffers(0, 2);
        glClearColor(0, 0, 0, 1f);
        glClear(GL_COLOR_BUFFER_BIT);
        for (int i = 0; i < bloomBuffers.length; i++) {
            int mipWidth = width >> (i + 1);
            int mipHeight = height >> (i + 1);
            if (mipWidth < 15 || mipHeight < 7)
                break;

            bloomBuffers[i] = OpenGlHelper.func_153165_e();
            GL30.glBindFramebuffer(GL_FRAMEBUFFER, bloomBuffers[i]);

            bloomTextures[i] = TextureUtil.glGenTextures();
            glBindTexture(GL_TEXTURE_2D, bloomTextures[i]);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, mipWidth, mipHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            OpenGlHelper.func_153188_a(GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, bloomTextures[i], 0);

            bloomTextures2[i] = TextureUtil.glGenTextures();
            glBindTexture(GL_TEXTURE_2D, bloomTextures2[i]);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, mipWidth, mipHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            OpenGlHelper.func_153188_a(GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, bloomTextures2[i], 0);

            int status = GL30.glCheckFramebufferStatus(GL_FRAMEBUFFER);
            if (status != GL_FRAMEBUFFER_COMPLETE)
                CommonProxy.LOGGER.error("Framebuffer " + i + " is not complete: " + status);

            glClearColor(0, 0, 0, 1f);
            glClear(GL_COLOR_BUFFER_BIT);
        }
        resetDrawBuffer();
        MAIN.bindFramebuffer(false);

        int status = GL30.glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE)
            CommonProxy.LOGGER.error("Framebuffer is not complete: " + status);

        DEPTH_TEXTURE = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, DEPTH_TEXTURE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);

        auraBuffer = OpenGlHelper.func_153165_e();
        GL30.glBindFramebuffer(GL_FRAMEBUFFER, auraBuffer);
        for (int i = 0; i < auraTextures.length; i++) {
            auraTextures[i] = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, auraTextures[i]);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            OpenGlHelper.func_153188_a(GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + i, GL_TEXTURE_2D, auraTextures[i], 0);
        }
        setupDepthAndStencil();
        status = GL30.glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE)
            CommonProxy.LOGGER.error("Aura framebuffer is not complete: " + status);
        glClearColor(0, 0, 0, 1f);
        glClear(GL_COLOR_BUFFER_BIT);

        GL30.glBindFramebuffer(GL_FRAMEBUFFER, previousBuffer);

        blankTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, blankTexture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
    }

    public static void delete() {
        if (!hasInitialized)
            return;
        for (int i = 0; i < bloomBuffers.length; i++) {
            if (bloomTextures[i] > 0)
                TextureUtil.deleteTexture(bloomTextures[i]);
            if (bloomTextures2[i] > 0)
                TextureUtil.deleteTexture(bloomTextures2[i]);
            if (bloomBuffers[i] > 0)
                OpenGlHelper.func_153174_h(bloomBuffers[i]);
        }

        OpenGlHelper.func_153174_h(auraBuffer);
        for (int i = 0; i < auraTextures.length; i++) {
            if (auraTextures[i] > 0)
                TextureUtil.deleteTexture(auraTextures[i]);
        }
        OpenGlHelper.func_153174_h(MISC_POST_PROCESSING_BUFFER);
        OpenGlHelper.func_153174_h(MAIN_BLOOM_BUFFER);

        bloomBuffers = new int[BLOOM_BUFFERS_LENGTH];
        bloomTextures = new int[bloomBuffers.length];
        bloomTextures2 = new int[bloomBuffers.length];

        if (SCENE_COPY_TEXTURE > 0) {
            TextureUtil.deleteTexture(SCENE_COPY_TEXTURE);
            SCENE_COPY_TEXTURE = 0;
        }

        TextureUtil.deleteTexture(MAIN_BLOOM_TEXTURE);
        TextureUtil.deleteTexture(DEPTH_TEXTURE);
        TextureUtil.deleteTexture(BLUR_TEXTURE);
        TextureUtil.deleteTexture(blankTexture);
    }

    public static void copyBuffer(int copyFBO, int pasteFBO, int width, int height, int bufferBits) {
        int previousBuffer = glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, copyFBO);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, pasteFBO);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, bufferBits, GL_NEAREST);

        int status = GL30.glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE)
            CommonProxy.LOGGER.error("Copying FBO " + pasteFBO + " is not complete: " + status);

        GL30.glBindFramebuffer(GL_FRAMEBUFFER, previousBuffer);
    }

    public static void saveTextureToPNG(int textureID) {
        if (Minecraft.getMinecraft().isGamePaused())
            return;

        glBindTexture(GL_TEXTURE_2D, textureID);
        int width = (int) GL11.glGetTexLevelParameterf(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = (int) GL11.glGetTexLevelParameterf(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = (x + (height - y - 1) * width) * 4;
                int r = buffer.get(index) & 0xFF;
                int g = buffer.get(index + 1) & 0xFF;
                int b = buffer.get(index + 2) & 0xFF;
                int a = buffer.get(index + 3) & 0xFF;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, argb);
            }
        }

        String desktopPath = System.getProperty("user.home") + "/Desktop/image/";
        String filename = desktopPath + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + "_" + textureID + ".png";
        File file = new File(filename);

        try {
            ImageIO.write(image, "PNG", file);
            System.out.println("Texture saved to: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to write PNG file: " + e.getMessage());
        }
    }

    /** Copies the bound main framebuffer into {@link #SCENE_COPY_TEXTURE} and returns it. */
    private static int copyScene(int width, int height) {
        if (SCENE_COPY_TEXTURE <= 0)
            SCENE_COPY_TEXTURE = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, SCENE_COPY_TEXTURE);
        if (sceneCopyWidth != width || sceneCopyHeight != height) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            sceneCopyWidth = width;
            sceneCopyHeight = height;
        }
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        return SCENE_COPY_TEXTURE;
    }

    private static boolean detectStencil() {
        if (OPTIFINE)
            return true;
        try {
            Framebuffer buffer = getMainBuffer();
            int previous = glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            boolean stencil;
            if (buffer != null && buffer.framebufferObject >= 0) {
                glBindFramebuffer(GL_FRAMEBUFFER, buffer.framebufferObject);
                stencil = glGetFramebufferAttachmentParameteri(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE) != GL_NONE;
            } else {
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                stencil = glGetFramebufferAttachmentParameteri(GL_FRAMEBUFFER, GL_STENCIL, GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE) > 0;
            }
            glBindFramebuffer(GL_FRAMEBUFFER, previous);
            return stencil;
        } catch (Throwable t) {
            return true;
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, PostProcessing.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static Framebuffer getMainBuffer() {
        return Minecraft.getMinecraft().getFramebuffer();
    }

    public static void updateViewportDimensions() {
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        glGetInteger(GL_VIEWPORT, viewport);
        VIEWPORT_WIDTH = viewport.get(2);
        VIEWPORT_HEIGHT = viewport.get(3);
    }

    public static void drawToBuffers(int... colorBuffers) {
        IntBuffer buffer = BufferUtils.createIntBuffer(colorBuffers.length);
        for (int colorBuffer : colorBuffers) {
            if (colorBuffer > 15) continue;
            buffer.put(GL30.GL_COLOR_ATTACHMENT0 + colorBuffer);
        }
        buffer.flip();
        GL20.glDrawBuffers(buffer);
    }

    public static void resetDrawBuffer() {
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
    }

    public static void renderQuad(int textureID, float startX, float startY, float width, float height) {
        Tessellator tessellator = Tessellator.instance;
        if (textureID != -1) glBindTexture(GL_TEXTURE_2D, textureID);

        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(startX, startY, 0, 0, 1);
        tessellator.addVertexWithUV(startX, height, 0, 0, 0);
        tessellator.addVertexWithUV(width, height, 0, 1, 0);
        tessellator.addVertexWithUV(width, startY, 0, 1, 1);
        tessellator.draw();

        glBindTexture(GL_TEXTURE_2D, 0);
    }
}
