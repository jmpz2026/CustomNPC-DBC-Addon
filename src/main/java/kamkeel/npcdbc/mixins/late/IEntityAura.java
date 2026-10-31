package kamkeel.npcdbc.mixins.late;

import JinRyuu.DragonBC.common.Npcs.EntityAura2;
import kamkeel.npcdbc.constants.enums.EnumAuraTypes2D;
import kamkeel.npcdbc.data.IAuraData;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Unique;

public interface IEntityAura {
    boolean isEnhancedRendering();

    float getState();

    void setState(float ok);

    boolean hasLightning();

    @Unique
    void setType2D(EnumAuraTypes2D types2D);

    @Unique
    EnumAuraTypes2D getType2D();

    void setHasLightning(boolean hasLightning);

    int getLightningColor();

    void setLightningColor(int lightningColor);

    @Unique
    int getLightningAlpha();

    @Unique
    void setLightningAlpha(int lightningAlpha);

    @Unique
    int getLightningSpeed();

    @Unique
    int getLightningIntensity();

    @Unique
    void setLightningSpeed(int lightningSpeed);

    @Unique
    void setLightningIntensity(int lightningIntensity);

    float getSize();

    void setSize(float getSize);

    @Unique
    EntityAura2 getParent();

    @Unique
    void setParent(EntityAura2 parent);

    @Unique
    boolean hasParent();

    @Unique
    void setIsKaioken(boolean is);

    @Unique
    boolean isKaioken();


    @Unique
    void setEntity(Entity entity);

    @Unique
    Entity getEntity();


    @Unique
    void setAuraData(IAuraData data);

    @Unique
    IAuraData getAuraData();

    @Unique
    int getRenderPass();

    @Unique
    void setGUIAura(boolean is);

    @Unique
    boolean isGUIAura();

    /** Draw this aura into the bloom buffer as well. */
    @Unique
    void setBloom(boolean bloom);

    @Unique
    boolean isBloom();

    /**
     * Aura started by {@link kamkeel.npcdbc.api.client.AuraApi}: its size comes from this power
     * instead of the owner's DBC data, and it is drawn even for the local player's name in first
     * person (cinematic actors share that name). Negative = a normal aura.
     */
    @Unique
    void setApiPower(float power);

    @Unique
    float getApiPower();
}

