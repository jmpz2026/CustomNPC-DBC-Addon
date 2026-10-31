package kamkeel.npcdbc.mixins.late.impl.dbc;

import kamkeel.npcdbc.client.render.LightmapState;
import JinRyuu.DragonBC.common.Npcs.EntityAura2;
import JinRyuu.DragonBC.common.Npcs.ModelAura;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import JinRyuu.DragonBC.common.Npcs.RenderAura2;
import JinRyuu.JRMCore.JRMCoreHDBC;
import JinRyuu.JRMCore.client.config.jrmc.JGConfigClientSettings;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.*;
import kamkeel.npcdbc.client.ClientConstants;
import kamkeel.npcdbc.client.gui.global.auras.SubGuiAuraDisplay;
import kamkeel.npcdbc.config.ConfigDBCClient;
import kamkeel.npcdbc.data.dbcdata.DBCData;
import kamkeel.npcdbc.mixins.late.IEntityAura;
import kamkeel.npcdbc.mixins.late.IRenderEntityAura2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import kamkeel.npcdbc.api.client.BloomApi;
import kamkeel.npcdbc.client.shader.PostProcessing;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.*;

@Mixin(value = RenderAura2.class, remap = false)
public class MixinRenderAura2 implements IRenderEntityAura2 {

    @Shadow
    private int lightVertN;

    @Inject(method = "renderAura", at = @At("HEAD"), cancellable = true)
    private void enableLightMap(EntityAura2 par1Entity, double parX, double parY, double parZ, float par8, float par9, CallbackInfo ci) {
        IEntityAura aura = (IEntityAura) par1Entity;

        boolean dontRender = aura.getRenderPass() == -1;
        if (dontRender)
            ci.cancel();

        if ((JGConfigClientSettings.CLIENT_DA12 || SubGuiAuraDisplay.useGUIAura) && dontRender) {
            this.lightning(par1Entity, parX, parY, parZ, par9, 1.0F, par1Entity.getAge(), par1Entity.getRot());
        }

    }

    /** Aura being drawn comes from AuraApi (set per renderAura call; auras do not nest). */
    @Unique
    private static boolean npcdbc$api;

    @Inject(method = "renderAura", at = @At("HEAD"))
    private void markApi(EntityAura2 par1Entity, double parX, double parY, double parZ, float par8, float par9, CallbackInfo ci) {
        npcdbc$api = ((IEntityAura) par1Entity).getApiPower() >= 0;
    }

    @Redirect(method = "renderAura", at = @At(value = "INVOKE", target = "LJinRyuu/DragonBC/common/Npcs/EntityAura2;getBol6()B"))
    private byte redirect5(EntityAura2 instance) {
        byte i = instance.getBol6();
        if (i == -10)
            return -1;
        return i;
    }

    @Redirect(method = "renderAura", at = @At(value = "FIELD", target = "LJinRyuu/JRMCore/client/config/jrmc/JGConfigClientSettings;CLIENT_DA12:Z"))
    private boolean redirect(EntityAura2 instance) {
        return JGConfigClientSettings.CLIENT_DA12 || SubGuiAuraDisplay.useGUIAura;
    }

    @Redirect(method = "renderAura", at = @At(value = "FIELD", target = "LJinRyuu/JRMCore/client/config/jrmc/JGConfigClientSettings;CLIENT_DA14:Z"))
    private boolean redirect2(EntityAura2 instance) {
        // AuraApi auras are asked for on purpose (a cinematic turns the 3D auras off for the rest).
        return JGConfigClientSettings.CLIENT_DA14 || SubGuiAuraDisplay.useGUIAura || npcdbc$api;
    }

    @Redirect(method = "renderAura", at = @At(value = "FIELD", target = "LJinRyuu/JRMCore/client/config/jrmc/JGConfigClientSettings;CLIENT_DA20:Z"))
    private boolean redirect3(EntityAura2 instance) {
        if (SubGuiAuraDisplay.useGUIAura)
            return false;
        return JGConfigClientSettings.CLIENT_DA20;
    }

    @Redirect(method = "func_tad", at = @At(value = "FIELD", target = "LJinRyuu/JRMCore/client/config/jrmc/JGConfigClientSettings;CLIENT_DA12:Z"))
    private boolean redirect4(EntityAura2 instance) {
        return JGConfigClientSettings.CLIENT_DA12 || SubGuiAuraDisplay.useGUIAura;
    }

    @Inject(method = "func_tad", at = @At(value = "INVOKE", target = "LJinRyuu/DragonBC/common/Npcs/RenderAura2;glColor4f(IF)V", ordinal = 0, shift = At.Shift.BEFORE))
    private void adjustFirstPersonOpacity(EntityAura2 par1Entity, double parX, double parY, double parZ, float par8, float par9, CallbackInfo ci, @Local(name = "p") LocalFloatRef opacity, @Local(name = "plyrSP") boolean isFirstPerson) {
        if (isFirstPerson)
            opacity.set(opacity.get() * ((float) ConfigDBCClient.FirstPerson3DAuraOpacity / 100));

    }

    @Shadow
    private void lightning(EntityAura2 e, double par2, double par4, double par6, float par9, float var20, float var13, boolean rot) {

    }

    @Redirect(method = "lightning", at = @At(value = "INVOKE", target = "LJinRyuu/DragonBC/common/Npcs/EntityAura2;getState()F"))
    private float setHasLightning(EntityAura2 instance) {
        IEntityAura aura = (IEntityAura) instance;
        if (aura.hasLightning())
            return 5f;
        return instance.getState();
    }

    @Redirect(method = "lightning", at = @At(value = "INVOKE", target = "LJinRyuu/DragonBC/common/Npcs/EntityAura2;getLightLivingTime()I"))
    private int setLightningSpeed(EntityAura2 instance) {
        IEntityAura aura = (IEntityAura) instance;
        if (aura.hasLightning() && aura.getLightningSpeed() != -1)
            return aura.getLightningSpeed();

        return instance.getLightLivingTime();
    }

    @Inject(method = "lightning", at = @At(value = "FIELD", target = "LJinRyuu/DragonBC/common/Npcs/RenderAura2;lightVertN:I", ordinal = 0, shift = At.Shift.AFTER))
    private void setLightningIntensity(EntityAura2 e, double par2, double par4, double par6, float par9, float var20, float var13, boolean rot, CallbackInfo ci, @Local(name = "nu2") LocalIntRef nu2) {
        IEntityAura aura = (IEntityAura) e;
        if (!aura.hasLightning())
            return;

        int intensity = aura.getLightningIntensity();

        if (aura.getLightningIntensity() > -1) {
            lightVertN = intensity;
            nu2.set(intensity);
        }

        if (SubGuiAuraDisplay.useGUIAura) {
            float scale = 0.55f;
            GL11.glScalef(scale, scale, scale);
            glStencilFunc(GL_ALWAYS, aura.getEntity().getEntityId() % 256, 0xFF);
            glStencilMask(0xFF);
        }
        Minecraft.getMinecraft().entityRenderer.disableLightmap(0);

    }

    @Inject(method = "lightning", at = @At("TAIL"))
    private void enableLightMap(EntityAura2 e, double par2, double par4, double par6, float par9, float var20, float var13, boolean rot, CallbackInfo ci) {
        IEntityAura aura = (IEntityAura) e;
        if (!aura.hasLightning())
            return;

        if (SubGuiAuraDisplay.useGUIAura) {
            glStencilFunc(GL_GREATER, aura.getEntity().getEntityId() % 256, 0xFF);
            glStencilMask(0x0);
        }

    }

    @Inject(method = "lightning", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;setColorRGBA_F(FFFF)V", ordinal = -1, shift = At.Shift.AFTER, remap = true))
    private void renderLightningColor(EntityAura2 e, double par2, double par4, double par6, float par9, float var20, float var13, boolean rot, CallbackInfo ci, @Local(name = "tessellator2") LocalRef<Tessellator> tessellator) {
        IEntityAura aura = (IEntityAura) e;
        if (!aura.hasLightning())
            return;

        int lightningColor = 0x25c9cf;
        int lightningAlpa = 255;

        if (aura.getLightningColor() > -1)
            lightningColor = aura.getLightningColor();
        if (aura.getLightningAlpha() > -1)
            lightningAlpa = aura.getLightningAlpha();

        if (lightningColor <= 1000)
            GL11.glDisable(GL11.GL_BLEND);

        tessellator.get().setColorRGBA_I(lightningColor, lightningAlpa);

    }

    //    @ModifyArgs(method = "func_tad(LJinRyuu/DragonBC/common/Npcs/EntityAura2;DDDFF)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glScalef(FFF)V", ordinal = 0))
//    private void setAuraSize(Args args, @Local(ordinal = 0) LocalRef<EntityAura2> entityAura, @Local(name = "cl3b") LocalBooleanRef hasColor3) {
//        EntityAura2 aur = entityAura.get();
//        IEntityAura aura = (IEntityAura) aur;
//        if (aura.getSize() != 1f) {
//            float size = aura.getSize();
//            float xSize = (float) args.get(0) * size;
//            float ySize = (float) args.get(1) * size;
//            float zSize = (float) args.get(2) * size;
//
//            args.set(0, xSize);
//            args.set(1, ySize);
//            args.set(2, zSize);
//
//
//        }
//        hasColor3.set(aur.getColL3() > 0 && aur.getTexL3().length() > 2);
//    }
    @Redirect(method = "func_tad(LJinRyuu/DragonBC/common/Npcs/EntityAura2;DDDFF)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glScalef(FFF)V", ordinal = 0))
    private void setAuraSize(float x, float y, float z, @Local(ordinal = 0, argsOnly = true) EntityAura2 entityAura, @Local(name = "cl3b") LocalBooleanRef hasColor3) {
        IEntityAura aura = (IEntityAura) entityAura;
        if (aura.getSize() != 1f) {
            float size = aura.getSize();
            x *= size;
            y *= size;
            z *= size;

        }
        hasColor3.set(entityAura.getColL3() > 0 && entityAura.getTexL3().length() > 2);
        GL11.glScalef(x, y, z);
    }

//    @ModifyArgs(method = "func_tad(LJinRyuu/DragonBC/common/Npcs/EntityAura2;DDDFF)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", ordinal = 0))
//    private void fixAuraOffset(Args args, @Local(ordinal = 0) LocalRef<EntityAura2> entityAura, @Local(ordinal = 1) LocalDoubleRef parY) {
//        IEntityAura aura = (IEntityAura) entityAura.get();
//        if (aura.getSize() != 1f) {
//            float fixedOffset = (float) (parY.get() + 3.0F * aura.getSize());
//            args.set(1, fixedOffset);
//        }
//        Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
//
//    }

    @Redirect(method = "func_tad(LJinRyuu/DragonBC/common/Npcs/EntityAura2;DDDFF)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", ordinal = 0))
    private void fixAuraOffset(float x, float y, float z, @Local(ordinal = 0) EntityAura2 entityAura, @Local(ordinal = 1) double parY) {
        IEntityAura aura = (IEntityAura) entityAura;
        if (aura.getSize() != 1f) {
            y = (float) (parY + 3.0F * aura.getSize());
        }
        Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
        GL11.glTranslatef(x, y, z);
    }

//    @ModifyArgs(method = "func_tad(LJinRyuu/DragonBC/common/Npcs/EntityAura2;DDDFF)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", ordinal = 1))
//    private void fixAuraRotationOffset_PRE(Args args, @Local(ordinal = 0) LocalRef<EntityAura2> entityAura, @Local(ordinal = 1) LocalDoubleRef parY) {
//        IEntityAura aura = (IEntityAura) entityAura.get();
//        if (aura.getSize() != 1f) {
//            float fixedOffset = (float) args.get(1) + ( (float) args.get(1) * (aura.getSize()-1) * 1.5f * 1.25f);
//            args.set(1, fixedOffset);
//        }
//
//    }

    @Redirect(method = "func_tad(LJinRyuu/DragonBC/common/Npcs/EntityAura2;DDDFF)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", ordinal = 1))
    private void fixAuraRotationOffset_PRE(float x, float y, float z, @Local(ordinal = 0) LocalRef<EntityAura2> entityAura, @Local(ordinal = 1) LocalDoubleRef parY) {
        IEntityAura aura = (IEntityAura) entityAura.get();
        if (aura.getSize() != 1f) {
            y = y + (y * (aura.getSize() - 1) * 1.5f * 1.25f);

        }
        GL11.glTranslatef(x, y, z);
    }

    @Redirect(method = "func_tad(LJinRyuu/DragonBC/common/Npcs/EntityAura2;DDDFF)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", ordinal = 2))
    private void fixAuraRotationOffset_POST(float x, float y, float z, @Local(ordinal = 0) LocalRef<EntityAura2> entityAura, @Local(ordinal = 1) LocalDoubleRef parY) {
        IEntityAura aura = (IEntityAura) entityAura.get();
        if (aura.getSize() != 1f) {
            y = y + (y * (aura.getSize() - 1) * 1.5f * (aura.getSize() > 1 ? 0.625f : 1.25f));
        }
        GL11.glTranslatef(x, y, z);
    }


    @Shadow
    private String mdid;

    /** Aura being drawn by func_tad (render thread, auras do not nest). */
    @Unique
    private static EntityAura2 npcdbc$current;

    /*
     * DBC builds mdid + ":" + tex + ".png" with mdid = "jinryuudragonbc". A texture with its own
     * domain ("dbrvfx:textures/...", from AuraDisplay.texture1..3) gives its domain for mdid and
     * the rest for tex. Done on the field and the getters, not on the ResourceLocation: DbrOptimizer
     * redirects that constructor. In func_tad, mdid is read for texL3, texL2 and tex, in that order.
     */
    @Unique
    private static String npcdbc$domain(String tex, String mdid) {
        int i = tex == null ? -1 : tex.indexOf(':');
        return i > 0 ? tex.substring(0, i) : mdid;
    }

    @Unique
    private static String npcdbc$path(String tex) {
        int i = tex == null ? -1 : tex.indexOf(':');
        return i > 0 ? tex.substring(i + 1) : tex;
    }

    @Redirect(method = "func_tad", at = @At(value = "FIELD", target = "LJinRyuu/DragonBC/common/Npcs/RenderAura2;mdid:Ljava/lang/String;", ordinal = 0))
    private String domainL3(RenderAura2 self) {
        return npcdbc$domain(npcdbc$current.getTexL3(), mdid);
    }

    @Redirect(method = "func_tad", at = @At(value = "FIELD", target = "LJinRyuu/DragonBC/common/Npcs/RenderAura2;mdid:Ljava/lang/String;", ordinal = 1))
    private String domainL2(RenderAura2 self) {
        return npcdbc$domain(npcdbc$current.getTexL2(), mdid);
    }

    @Redirect(method = "func_tad", at = @At(value = "FIELD", target = "LJinRyuu/DragonBC/common/Npcs/RenderAura2;mdid:Ljava/lang/String;", ordinal = 2))
    private String domainL1(RenderAura2 self) {
        return npcdbc$domain(npcdbc$current.getTex(), mdid);
    }

    @Redirect(method = "func_tad", at = @At(value = "INVOKE", target = "LJinRyuu/DragonBC/common/Npcs/EntityAura2;getTex()Ljava/lang/String;"))
    private String pathL1(EntityAura2 aura) {
        return npcdbc$path(aura.getTex());
    }

    @Redirect(method = "func_tad", at = @At(value = "INVOKE", target = "LJinRyuu/DragonBC/common/Npcs/EntityAura2;getTexL2()Ljava/lang/String;"))
    private String pathL2(EntityAura2 aura) {
        return npcdbc$path(aura.getTexL2());
    }

    @Redirect(method = "func_tad", at = @At(value = "INVOKE", target = "LJinRyuu/DragonBC/common/Npcs/EntityAura2;getTexL3()Ljava/lang/String;"))
    private String pathL3(EntityAura2 aura) {
        return npcdbc$path(aura.getTexL3());
    }

    /** Cinematic actors carry the local player's name: that is not the first-person aura. */
    @Redirect(method = "func_tad", at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"))
    private boolean apiNotFirstPerson(String name, Object owner) {
        return !npcdbc$api && name.equals(owner);
    }

    /*
     * Bloom only on layer 2 (the edge): a dark aura with a bright rim. Blooming every layer made
     * the whole aura glow and a near-black one came out pink. func_tad binds layer 2 right before
     * drawing it, so the last bound texture says which layer the next renderModel draws.
     */
    @Unique
    private static boolean npcdbc$layer2;

    @Inject(method = "func_tad", at = @At("HEAD"))
    private void startAura(EntityAura2 par1Entity, double parX, double parY, double parZ, float par8, float par9, CallbackInfo ci) {
        npcdbc$current = par1Entity;
        npcdbc$layer2 = false;
    }

    @Redirect(method = "func_tad", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", remap = true))
    private void bindLayer(TextureManager tm, ResourceLocation loc) {
        String l2 = npcdbc$path(npcdbc$current.getTexL2());
        npcdbc$layer2 = l2 != null && l2.length() > 2 && loc.getResourcePath().equals(l2 + ".png");
        tm.bindTexture(loc);
    }

    @Redirect(method = "func_tad", at = @At(value = "INVOKE", target = "LJinRyuu/DragonBC/common/Npcs/ModelAura;renderModel(Lnet/minecraft/entity/Entity;FFFF)V"))
    private void renderLayer(ModelAura model, Entity e, float f, float r, float w, float s) {
        model.renderModel(e, f, r, w, s);
        if (!npcdbc$layer2 || !((IEntityAura) npcdbc$current).isBloom() || ClientConstants.renderingGUI || !BloomApi.begin())
            return;
        // Second pass into the bloom target only, fainter: the aura covers a large area and the
        // edge at full alpha washes the whole screen in its colour.
        npcdbc$color.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, npcdbc$color);
        float cr = npcdbc$color.get(0), cg = npcdbc$color.get(1), cb = npcdbc$color.get(2), ca = npcdbc$color.get(3);
        PostProcessing.drawToBuffers(2);
        GL11.glColor4f(cr, cg, cb, ca * NPCDBC_BLOOM_ALPHA);
        model.renderModel(e, f, r, w, s);
        GL11.glColor4f(cr, cg, cb, ca);
        BloomApi.end();
    }

    @Unique
    private static final float NPCDBC_BLOOM_ALPHA = 0.35f;
    @Unique
    private static final java.nio.FloatBuffer npcdbc$color = org.lwjgl.BufferUtils.createFloatBuffer(16);

    /** Lightmap state before func_tad turned it off (see LightmapState). Auras do not nest. */
    @Unique
    private static boolean npcdbc$lightmapBefore = true;

    @Inject(method = "func_tad", at = @At("HEAD"))
    private void saveLight(EntityAura2 par1Entity, double parX, double parY, double parZ, float par8, float par9, CallbackInfo ci) {
        npcdbc$lightmapBefore = LightmapState.isOn();
    }

    @Inject(method = "func_tad", at = @At("TAIL"))
    private void enableLight(EntityAura2 par1Entity, double parX, double parY, double parZ, float par8, float par9, CallbackInfo ci) {
        LightmapState.restore(npcdbc$lightmapBefore);
    }

    @Inject(method = "func_tad(LJinRyuu/DragonBC/common/Npcs/EntityAura2;DDDFF)V", at = @At(value = "INVOKE", target = "LJinRyuu/DragonBC/common/Npcs/EntityAura2;getState2()F", ordinal = 0, shift = At.Shift.BEFORE))
    private void fixAuraSize(EntityAura2 par1Entity, double parX, double parY, double parZ, float par8, float par9, CallbackInfo ci, @Local(name = "s1") LocalFloatRef s1, @Local(name = "s") LocalFloatRef s, @Local(name = "cr") LocalFloatRef cr) {
        IEntityAura aura = (IEntityAura) par1Entity;
        if (aura.getApiPower() >= 0) {
            s.set(aura.getApiPower());
            return;
        }
        Entity auraOwner = aura.getEntity();
        if (auraOwner instanceof EntityPlayer) {
            DBCData dbcData = DBCData.get((EntityPlayer) auraOwner);
            float size = JRMCoreHDBC.DBCsizeBasedOnRace2(dbcData.Race, dbcData.State);

            if (dbcData.addonFormID > -1)
                s1.set(8.0f * size);


            s.set(s.get() == 0 ? 1 : s.get() * Math.min(dbcData.Release, 100) * 0.01f);
            return;
        }
    }

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true)
    private void disableRendering(Entity par1Entity, double par2, double par4, double par6, float par8, float par9, CallbackInfo ci) {
        IEntityAura aura = (IEntityAura) par1Entity;
        if (aura.isEnhancedRendering())
            ci.cancel();
    }

    @Unique
    public void renderParticle(EntityAura2 aura, float partialTicks) {
        double interPosX = (aura.lastTickPosX + (aura.posX - aura.lastTickPosX) * (double) partialTicks) - (ClientConstants.renderingGUI ? 0 : RenderManager.renderPosX);
        double interPosY = (aura.lastTickPosY + (aura.posY - aura.lastTickPosY) * (double) partialTicks) - (ClientConstants.renderingGUI ? -0.05 : RenderManager.renderPosY);
        double interPosZ = (aura.lastTickPosZ + (aura.posZ - aura.lastTickPosZ) * (double) partialTicks) - (ClientConstants.renderingGUI ? 0 : RenderManager.renderPosZ);
        float interYaw = aura.prevRotationYaw + (aura.rotationYaw - aura.prevRotationYaw) * partialTicks;


        ((RenderAura2) (Object) this).renderAura(aura, interPosX, interPosY, interPosZ, interYaw, partialTicks);
    }
}
