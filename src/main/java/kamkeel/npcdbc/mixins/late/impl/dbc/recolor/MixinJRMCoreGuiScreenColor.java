package kamkeel.npcdbc.mixins.late.impl.dbc.recolor;

import JinRyuu.JRMCore.JRMCoreGuiScreen;
import kamkeel.npcdbc.client.ColorMode;
import kamkeel.npcdbc.config.ConfigDBCClient;
import kamkeel.npcdbc.util.Mente;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = JRMCoreGuiScreen.class, remap = false)

public class MixinJRMCoreGuiScreenColor extends GuiScreen {

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"), remap = true)
    private int drawScreenFix(FontRenderer instance, String text, int x, int y, int color) {
        text = Mente.filtrar(text);
        if (text != null && text.isEmpty()) return x;
        if (ConfigDBCClient.EnhancedGui || !ConfigDBCClient.EnableDebugStatSheetSwitching) {
            return instance.drawString(ColorMode.skimColors(text), x, y, ColorMode.textColor(), ConfigDBCClient.DarkMode);
        }
        return instance.drawString(text, x, y, color);
    }

    // DBR: the skills tab header "TP and Mind Cost:" without the retired Mind
    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", target = "LJinRyuu/JRMCore/JRMCoreGuiScreen;drawStringWithBorder(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V", remap = false), remap = true)
    private void drawBorderFix(FontRenderer font, String text, int x, int y, int color) {
        String t = Mente.filtrar(text);
        if (t == null || t.isEmpty()) return;
        if (!t.equals(text)) x += font.getStringWidth(text) - font.getStringWidth(t);
        JRMCoreGuiScreen.drawStringWithBorder(font, t, x, y, color);
    }

    @Redirect(method = "drawHUD_helpgmodeselect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I", remap = true))
    private int HelpSelect(FontRenderer instance, String text, int x, int y, int color) {
        if (ConfigDBCClient.EnhancedGui || !ConfigDBCClient.EnableDebugStatSheetSwitching) {
            return instance.drawString(ColorMode.skimColors(text), x, y, ColorMode.textColor(), ConfigDBCClient.DarkMode);
        }
        return instance.drawString(text, x, y, color);
    }

    @Redirect(method = "drawHUD_instantTransmissionPicker", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I", remap = true))
    private int instantTrans(FontRenderer instance, String text, int x, int y, int color) {
        if (ConfigDBCClient.EnhancedGui || !ConfigDBCClient.EnableDebugStatSheetSwitching) {
            return instance.drawString(ColorMode.skimColors(text), x, y, ColorMode.textColor(), ConfigDBCClient.DarkMode);
        }
        return instance.drawString(text, x, y, color);
    }

    @Redirect(method = "drawHUD_clntsett", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I", remap = true))
    private int drawHUD_clntsett(FontRenderer instance, String text, int x, int y, int color) {
        if (ConfigDBCClient.EnhancedGui || !ConfigDBCClient.EnableDebugStatSheetSwitching) {
            return instance.drawString(ColorMode.skimColors(text), x, y, ColorMode.textColor(), ConfigDBCClient.DarkMode);
        }
        return instance.drawString(text, x, y, color);
    }

    @Redirect(method = "drawDetails(Ljava/lang/String;Ljava/lang/String;IIIILnet/minecraft/client/gui/FontRenderer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I", remap = true))
    private static int drawDetails(FontRenderer instance, String text, int x, int y, int color) {
        if (ConfigDBCClient.EnhancedGui || !ConfigDBCClient.EnableDebugStatSheetSwitching) {
            return instance.drawString(ColorMode.skimColors(text), x, y, ColorMode.textColor(), ConfigDBCClient.DarkMode);
        }
        return instance.drawString(text, x, y, color);
    }

    @Redirect(method = "current", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I", remap = true))
    private int current(FontRenderer instance, String text, int x, int y, int color) {
        if (ConfigDBCClient.EnhancedGui || !ConfigDBCClient.EnableDebugStatSheetSwitching) {
            return instance.drawString(ColorMode.skimColors(text), x, y, ColorMode.textColor(), ConfigDBCClient.DarkMode);
        }
        return instance.drawString(text, x, y, color);
    }

    @Inject(method = "textLevel", at = @At(value = "HEAD"), cancellable = true)
    private void textLevel(int lvl, CallbackInfoReturnable<String> cir) {
        if (ConfigDBCClient.EnhancedGui || !ConfigDBCClient.EnableDebugStatSheetSwitching) {
            if (ConfigDBCClient.DarkMode) {
                cir.setReturnValue("§7(lvl: " + lvl + ")");
            }
        }
    }
}
