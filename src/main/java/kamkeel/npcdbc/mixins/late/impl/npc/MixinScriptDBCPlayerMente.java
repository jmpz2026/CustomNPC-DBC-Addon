package kamkeel.npcdbc.mixins.late.impl.npc;

import kamkeel.npcdbc.CommonProxy;
import kamkeel.npcdbc.util.Mente;
import noppes.npcs.scripted.entity.ScriptDBCPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scripts (IDBCPlayer.addBonusAttribute and friends) cannot add bonuses to the retired Mind attribute. */
@Mixin(value = ScriptDBCPlayer.class, remap = false)
public abstract class MixinScriptDBCPlayerMente {

    @Inject(method = "bonusAttribute(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;DZ)Ljava/lang/String;",
        at = @At("HEAD"), cancellable = true)
    private void blockMindBonus(String action, String stat, String bonusID, String operation, double value, boolean end,
                                CallbackInfoReturnable<String> cir) {
        if (action == null || !Mente.esMente(stat)) return;
        String a = action.toLowerCase();
        if (a.equals("add") || a.equals("addto") || a.equals("set")) {
            CommonProxy.LOGGER.warn("Ignored Mind bonus '" + bonusID + "' (" + a + "): Mind is retired");
            cir.setReturnValue("");
        }
    }
}
