package kamkeel.npcdbc.mixins.late.impl.dbc;

import JinRyuu.JRMCore.JRMCoreH;
import kamkeel.npcdbc.util.Mente;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mind is retired. Everything that writes attributes through {@code JRMCoreH.setInt} (/jrmca set|add|max,
 * scripts, other mods) may not raise it: the new value is capped at the higher of the starting Mind
 * and the current one, so a value that is still waiting for its refund is not lost.
 */
@Mixin(value = JRMCoreH.class, remap = false)
public abstract class MixinJRMCoreHMente {

    @ModifyVariable(method = "setInt(ILnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int capMind(int value, int value2, EntityPlayer player, String key) {
        if (player == null || player.worldObj == null || player.worldObj.isRemote || !Mente.NBT.equals(key)) return value;
        int tope = Math.max(Mente.inicial(player), JRMCoreH.getInt(player, Mente.NBT));
        return Math.min(value, tope);
    }
}
