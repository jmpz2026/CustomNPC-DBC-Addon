package kamkeel.npcdbc.mixins.late.impl.dbc;

import JinRyuu.JRMCore.JRMCoreH;
import JinRyuu.JRMCore.JRMCorePacHanS;
import com.llamalad7.mixinextras.sugar.Local;
import kamkeel.npcdbc.data.dbcdata.DBCData;
import kamkeel.npcdbc.util.Mente;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JRMCorePacHanS.class, remap = false)
public class MixinJRMCPacketHandlerServer {

    /** Mind is retired: the attribute upgrade packet for index 4 (any multiplier) does nothing. */
    @Inject(method = "handleUpgrade(BLnet/minecraft/entity/player/EntityPlayer;)V", at = @At("HEAD"), cancellable = true)
    private void blockMindUpgrade(byte b, EntityPlayer p, CallbackInfo ci) {
        if (((b % 6) + 6) % 6 == Mente.INDICE) {
            Mente.avisar(p);
            ci.cancel();
        }
    }

    @Redirect(method = "handleStats3", at = @At(value = "INVOKE", target = "LJinRyuu/JRMCore/JRMCoreH;canAffordSkill(II)Z"))
    private boolean addAddonMindBonus(int mindAttribute, int haveSkillLvls, @Local(ordinal = 0, argsOnly = true) EntityPlayer player) {
        return JRMCoreH.canAffordSkill(mindAttribute + DBCData.get(player).calculateMindBonuses(), haveSkillLvls);
    }
}
