package kamkeel.npcdbc.mixins.late.impl.dbc.client;

import JinRyuu.DragonBC.common.Gui.DBCTalkGui;
import kamkeel.npcdbc.util.Mente;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The masters' skill screen without the retired Mind: no "Available Mind" line and no "Mind: N" in the costs. */
@Mixin(DBCTalkGui.class)
public class MixinDBCTalkGuiMente {

    @Redirect(method = {"drawScreen"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"))
    private int filterScreen(FontRenderer font, String text, int x, int y, int color) {
        return draw(font, text, x, y, color);
    }

    @Redirect(method = {"sklLrn(III)V", "sklLrn(IIIZ)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I", remap = true), remap = false)
    private int filterSkills(FontRenderer font, String text, int x, int y, int color) {
        return draw(font, text, x, y, color);
    }

    private static int draw(FontRenderer font, String text, int x, int y, int color) {
        String t = Mente.filtrar(text);
        if (t != null && t.isEmpty()) return x;
        return font.drawString(t, x, y, color);
    }
}
