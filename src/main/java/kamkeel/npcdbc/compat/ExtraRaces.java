package kamkeel.npcdbc.compat;

import JinRyuu.JRMCore.JRMCoreH;
import kamkeel.npcdbc.CommonProxy;
import kamkeel.npcdbc.constants.DBCRace;
import kamkeel.npcdbc.data.PlayerDBCInfo;
import kamkeel.npcdbc.data.dbcdata.DBCData;
import kamkeel.npcdbc.util.DBCUtils;
import kamkeel.npcdbc.util.PlayerDataUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Map;

/**
 * Single entry point for the extra races that DbrRazas adds to JRMCore (race
 * 6 onwards). Every per-race switch in this addon (DBCForm, DBCData, the
 * ascend mixins) has one {@code isExtraRace} branch that delegates here, so a
 * new extra race or rule only touches this class.
 *
 * Rules (which forms are unlocked, attributes) are always asked to DbrRazas
 * through its public RazaApi, never copied here. Forms are JRMCore's own
 * table for the race: state = index, 0 = base. Soft dependency: without
 * DbrRazas an extra race has no forms in the wheel and its base attribute.
 *
 * The other direction also lives here: DbrRazas replaces JRMCore's ki regen
 * call for its races, so the divine-drain guard this addon injects into
 * getKiRegen* never runs for them, and its ascend key needs the form picked in
 * the wheel. DbrRazas calls the public helpers at the bottom.
 */
public final class ExtraRaces {

    private static final Logger LOG = LogManager.getLogger("npcdbc-ExtraRaces");
    private static final String API = "dbr.machitos.razas.api.RazaApi";

    private static final SoftMethod ATTRIBUTE = new SoftMethod(LOG, API, "getAttribute", MethodType.methodType(int.class,
        EntityPlayer.class, int[].class, int.class, int.class, int.class, int.class, int.class, int.class, boolean.class));
    private static final SoftMethod SELECTABLE = new SoftMethod(LOG, API, "isFormSelectable",
        MethodType.methodType(boolean.class, int.class, int.class, int.class, int.class));

    private ExtraRaces() {}

    // ------------------------------------------------------------------ forms

    /** Racial form of the race (1 .. last index of its JRMCore table; 0 is base). */
    public static boolean isForm(int race, int form) {
        return DBCRace.isExtraRace(race) && form >= 1 && form < JRMCoreH.trans[race].length;
    }

    /** Lang key of the form name (DbrRazas ships it and LangOverride localizes it). */
    public static String formLangKey(int race, int form) {
        return JRMCoreH.tjrmc + "." + JRMCoreH.TransNms[race][form];
    }

    /** Form name for menus, in the client's language. */
    public static String getMenuName(int race, int form) {
        return isForm(race, form) ? "§d" + StatCollector.translateToLocal(formLangKey(race, form)) : null;
    }

    /** All racial forms of the race, for editors (no unlock check). */
    public static void putAllForms(Map<Integer, String> forms, int race) {
        for (int i = 1; isForm(race, i); i++)
            forms.put(i, getMenuName(race, i));
    }

    /**
     * Forms the player can pick, same rule as DbrRazas' X selector.
     * {@code racialSkill} is the value DBCData uses (SklLvlX - 1).
     */
    public static void putUnlockedForms(Map<Integer, String> forms, int race, int racialSkill, int godSkill) {
        for (int i = 1; isForm(race, i); i++) {
            if (isFormSelectable(race, i, racialSkill + 1, godSkill))
                forms.put(i, getMenuName(race, i));
        }
    }

    /** Previous form in the wheel (the one JRMCore descends to), or -1 at the bottom of a branch. */
    public static int getParent(int race, int form) {
        if (!isForm(race, form))
            return -1;
        int parent = JRMCoreH.transformationDescendToFormID[race][form];
        return parent > 0 ? parent : -1;
    }

    /** Next unlocked form in the wheel (the first one that descends to this one), or -1. */
    public static int getChild(int race, int form, DBCData data) {
        for (int i = 1; isForm(race, i); i++) {
            if (i != form && JRMCoreH.transformationDescendToFormID[race][i] == form && data.isDBCFormUnlocked(i))
                return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ ascend

    /**
     * Server, form picked in the wheel: store it as the X selection (setting
     * 1), which is what DbrRazas ascends to. Settings is refreshed because
     * saveNBTData writes that field back over the compound.
     */
    public static void selectForm(DBCData data, int form) {
        if (!isForm(data.Race, form))
            return;
        data.setSetting(1, form);
        data.Settings = data.getRawCompound().getString("jrmcSettings");
    }

    /**
     * Server, handleDBCascend with a form picked in the wheel. Returns the
     * state the ascend must start from: base, so DbrRazas jumps straight to
     * the picked form from any form, like the original races do with the wheel.
     */
    public static byte prepareAscend(DBCData data, int form) {
        selectForm(data, form);
        return 0;
    }

    // ------------------------------------------------------------------ attributes

    /**
     * Attribute of an extra race in its current racial form, before the custom
     * form multipliers. {@code neutralizeBase} sets the base form multiplier to
     * 1, like replaceOldMulti does for the original races when the custom form
     * is not vanilla stackable.
     */
    public static int getAttribute(EntityPlayer player, int[] currAttributes, int attribute, int state, int race,
                                   int skillX, int mysticLvl, int powerType, boolean neutralizeBase) {
        MethodHandle h = ATTRIBUTE.get();
        if (h != null) {
            try {
                return (int) h.invokeExact(player, currAttributes, attribute, state, race, skillX, mysticLvl, powerType,
                    neutralizeBase);
            } catch (Throwable t) {
                ATTRIBUTE.disable(t);
            }
        }
        return currAttributes[attribute];
    }

    /** DbrRazas' unlock rule. {@code racialLevel} is the raw SklLvlX value. */
    private static boolean isFormSelectable(int race, int form, int racialLevel, int godSkillLevel) {
        MethodHandle h = SELECTABLE.get();
        if (h != null) {
            try {
                return (boolean) h.invokeExact(race, form, racialLevel, godSkillLevel);
            } catch (Throwable t) {
                SELECTABLE.disable(t);
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ called by DbrRazas

    /** Client: DBC form picked in the form wheel, or -1. */
    public static int getClientWheelForm() {
        PlayerDBCInfo info = PlayerDataUtil.getClientDBCInfo();
        return info != null ? info.selectedDBCForm : -1;
    }

    /** True if the player being ticked is in a custom form that cancels the racial ki drain. */
    public static boolean isFormDrainCancelled() {
        EntityPlayer player = CommonProxy.getCurrentJRMCTickPlayer();
        if (player == null) {
            return false;
        }
        PlayerDBCInfo info = PlayerDataUtil.getDBCInfo(player);
        if (!info.isInCustomForm()) {
            return false;
        }
        return !info.getCurrentForm().stackable.isVanillaStackable();
    }

    /** Mark the start of a ki drain calculation (Divine is not applied to it). */
    public static void beginKiDrain() {
        DBCUtils.calculatingKiDrain = true;
    }

    public static void endKiDrain() {
        DBCUtils.calculatingKiDrain = false;
    }
}
