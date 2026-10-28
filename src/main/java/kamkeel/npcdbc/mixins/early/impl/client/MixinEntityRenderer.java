package kamkeel.npcdbc.mixins.early.impl.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import kamkeel.npcdbc.client.ClientConstants;
import kamkeel.npcdbc.client.render.LightmapState;
import kamkeel.npcdbc.client.shader.PostProcessing;
import kamkeel.npcdbc.scripted.DBCPlayerEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

@Mixin(value = EntityRenderer.class)
public class MixinEntityRenderer {
    @Shadow
    public Minecraft mc;

    @Inject(method = "renderWorld", at = @At(value = "HEAD"))
    private void startRenderingWorld(float p_78471_1_, long p_78471_2_, CallbackInfo ci) {
        ClientConstants.renderingWorld = true;
    }

    @Inject(method = "renderWorld", at = @At(value = "RETURN"))
    private void endRenderingWorld(float p_78471_1_, long p_78471_2_, CallbackInfo ci) {
        ClientConstants.renderingWorld = false;
    }

    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;debugViewDirection:I", ordinal = 0, shift = At.Shift.BEFORE))
    private void captureDefaultMatrices(float partialTick, long idk, CallbackInfo info, @Local(name = "frustrum") LocalRef<Frustrum> frustrum) {
        // Nothing between ActiveRenderInfo.updateRenderInfo and here changes the camera matrices
        // (sky, clouds and terrain push and pop), so copy what it read instead of a GL readback.
        copy(AccessorActiveRenderInfo.npcdbc$getModelview(), PostProcessing.DEFAULT_MODELVIEW);
        copy(AccessorActiveRenderInfo.npcdbc$getProjection(), PostProcessing.DEFAULT_PROJECTION);
    }

    private static void copy(FloatBuffer src, FloatBuffer dst) {
        for (int i = 0; i < 16; i++)
            dst.put(i, src.get(i));
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderHelper;enableStandardItemLighting()V", ordinal = 1, shift = At.Shift.AFTER))
    private void secondRendPass(float partialTick, long idk, CallbackInfo info, @Local(name = "frustrum") LocalRef<Frustrum> frustrum) {
        mc.mcProfiler.endStartSection("NPCDBCEntities");
        ForgeHooksClient.setRenderPass(ClientConstants.MiddleRenderPass);
        glDepthMask(true);
        // Runs after EntityRenderer turned the lightmap off; leave it as it was (see LightmapState).
        boolean lightmap = LightmapState.isOn();
        this.mc.renderGlobal.renderEntities(this.mc.renderViewEntity, frustrum.get(), partialTick);
        LightmapState.restore(lightmap);
        ForgeHooksClient.setRenderPass(-1);
    }


    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItemInFirstPerson(F)V", shift = At.Shift.BEFORE), cancellable = true)
    public void renderItemEvent(float p_78476_1_, int p_78476_2_, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new DBCPlayerEvent.RenderArmEvent.Item(mc.thePlayer, null, mc.timer.renderPartialTicks))) {
            ci.cancel();
            glPopMatrix();
        }
        PostProcessing.bloom(1.5f, true);
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/OpenGlHelper;shadersSupported:Z", shift = At.Shift.BEFORE))
    private void post(float p_78480_1_, CallbackInfo ci) {
        PostProcessing.postProcess();
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;drawScreen(IIF)V", shift = At.Shift.BEFORE))
    private void preGUIRender(float p_78480_1_, CallbackInfo ci) {
        ClientConstants.renderingGUI = true;
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;drawScreen(IIF)V", shift = At.Shift.AFTER))
    private void postGUIRender(float p_78480_1_, CallbackInfo ci) {
        ClientConstants.renderingGUI = false;
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand(FI)V", shift = At.Shift.BEFORE))
    private void preArmRender(float p_78471_1_, long p_78471_2_, CallbackInfo ci) {
        ClientConstants.renderingArm = true;
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand(FI)V", shift = At.Shift.AFTER))
    private void postArmRender(float p_78471_1_, long p_78471_2_, CallbackInfo ci) {
        ClientConstants.renderingArm = false;
    }
}
