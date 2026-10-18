package kamkeel.npcdbc.mixins.late.impl.dbc;

import JinRyuu.JRMCore.ComJrmcaBonus;
import kamkeel.npcdbc.util.Mente;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** /jrmcabonus add|addto|set on Mind does nothing: the attribute is retired. get, remove and clear still work. */
@Mixin(value = ComJrmcaBonus.class, remap = false)
public abstract class MixinComJrmcaBonus {

    @Inject(method = "processCommand", at = @At("HEAD"), cancellable = true, remap = true)
    private void blockMindBonus(ICommandSender sender, String[] args, CallbackInfo ci) {
        if (args == null || args.length < 2 || !Mente.esMente(args[1])) return;
        String modo = args[0].toLowerCase();
        if (modo.equals("add") || modo.equals("addto") || modo.equals("set")) {
            sender.addChatMessage(new ChatComponentTranslation("npcdbc.mente.bonus"));
            ci.cancel();
        }
    }
}
