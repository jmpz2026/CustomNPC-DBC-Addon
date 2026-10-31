package kamkeel.npcdbc.client;

import JinRyuu.JRMCore.JRMCoreH;
import kamkeel.npcdbc.constants.DBCRace;
import kamkeel.npcdbc.data.form.Form;
import kamkeel.npcdbc.util.Utility;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;

/**
 * Why a custom transformation did not happen, said as "what happened + what to do".
 * Every argument is already translated for the local player; names keep their colours and
 * the line goes back to red after each one.
 */
public final class TransformMessages {

    private static final String RED = "§c";

    private TransformMessages() {
    }

    public static void send(String key, Object... args) {
        Utility.sendMessage(Minecraft.getMinecraft().thePlayer, RED + StatCollector.translateToLocalFormatted(key, args));
    }

    /** Menu name of a form, colours included, followed by the reset to red. */
    public static String form(Form form) {
        if (form == null) return "?";
        String name = form.getMenuName();
        if (name == null || name.isEmpty()) name = form.getName();
        return name + RED;
    }

    /** Name of a DBC race form (jrmcState) as DBC shows it: "Supremo", "Super Saiyan"... */
    public static String dbcState(int race, int state) {
        try {
            String raw = JRMCoreH.TransNms[race][state];
            return orRaw(JRMCoreH.trl("jrmc", raw), raw);
        } catch (Throwable t) {
            return StatCollector.translateToLocalFormatted("npcdbc.msg.dbcStateN", state);
        }
    }

    /** Race a form is meant for: one race, all Saiyans or everybody. */
    public static String race(int race) {
        if (race == -1) return StatCollector.translateToLocal("npcdbc.msg.allRaces");
        if (race == DBCRace.ALL_SAIYANS) return StatCollector.translateToLocal("npcdbc.msg.allSaiyans");
        try {
            String raw = JRMCoreH.Races[race];
            return orRaw(JRMCoreH.trl("jrmc", raw), raw);
        } catch (Throwable t) {
            return String.valueOf(race);
        }
    }

    /** trl returns the key itself when the lang has no entry: show the raw name instead. */
    private static String orRaw(String translated, String raw) {
        if (translated == null || translated.isEmpty() || translated.startsWith(JRMCoreH.tjrmc + ".")) return raw;
        return translated;
    }
}
