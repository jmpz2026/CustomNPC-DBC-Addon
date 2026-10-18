package kamkeel.npcdbc.util;

import JinRyuu.JRMCore.JRMCoreH;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

import java.util.regex.Pattern;

/**
 * The Mind attribute (MND, index 4, {@code jrmcIntI}) is retired on this server: nobody can raise it,
 * nothing grants it and the GUIs do not show it. The value stays in the NBT and in JinRyuu's code.
 */
public final class Mente {

    public static final int INDICE = 4;
    public static final String NBT = "jrmcIntI";
    private static final Pattern COSTE_M = Pattern.compile(" M: [0-9.,]+");

    private Mente() {}

    /** Starting Mind of the player's race and class: the most a character may have. */
    public static int inicial(EntityPlayer p) {
        return JRMCoreH.attributeStart(JRMCoreH.getByte(p, "jrmcPwrtyp"), INDICE,
            JRMCoreH.getByte(p, "jrmcRace"), JRMCoreH.getByte(p, "jrmcClass"));
    }

    public static void avisar(EntityPlayer p) {
        if (p == null) return;
        p.addChatMessage(new ChatComponentTranslation("npcdbc.mente.retirada")
            .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
    }

    /** True if the attribute name of a command or script means Mind. */
    public static boolean esMente(String atributo) {
        if (atributo == null) return false;
        String a = atributo.trim().toLowerCase();
        return a.equals("mnd") || a.equals("mind") || a.equals("int");
    }

    /**
     * Text of the old JRMCore screens without the Mind parts: the "M: 10" of the skill costs, the
     * "Mind: 10" of the master's list and the "Available Mind" line. Empty string = do not draw.
     */
    public static String filtrar(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        if (texto.contains(JRMCoreH.trl("jrmc", "AvailableMind"))) return "";
        if (texto.equals("TP and Mind Cost:")) return "TP:";
        String mind = JRMCoreH.trl("jrmc", "Mind") + ": ";
        if (texto.trim().startsWith(mind.trim())) return "";
        String t = COSTE_M.matcher(texto).replaceAll("");
        String talk = JRMCoreH.trl("dbc.talkgui.mind");
        int i = talk.isEmpty() ? -1 : t.indexOf(" " + talk + ": ");
        if (i >= 0) t = t.substring(0, i);
        return t;
    }
}
