package kamkeel.npcdbc.compat;

import kamkeel.npcdbc.CommonProxy;
import kamkeel.npcdbc.data.PlayerDBCInfo;
import kamkeel.npcdbc.util.DBCUtils;
import kamkeel.npcdbc.util.PlayerDataUtil;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Bridge to the extra races that DbrRazas adds to JRMCore (race 6 onwards).
 *
 * JRMCore has one attribute formula per race and this addon repeats that
 * switch for custom forms; an extra race has no formula here, so it is asked
 * to DbrRazas through reflection. Soft dependency: without DbrRazas loaded the
 * extra race gets its base attribute and nothing crashes.
 *
 * The other direction also lives here: DbrRazas replaces JRMCore's ki regen
 * call for its races, so the divine-drain guard this addon injects into
 * getKiRegen* never runs for them. DbrRazas calls these public helpers instead.
 */
public final class ExtraRaces {

    private static final Logger LOG = LogManager.getLogger("npcdbc-ExtraRaces");
    private static final String API = "dbr.machitos.razas.api.RazaApi";
    private static final String LOGIC = "dbr.machitos.razas.logic.ExtraRaceLogic";

    private static MethodHandle attributeHandle;
    private static boolean lookedUp;
    private static boolean failed;

    private static MethodHandle selectableHandle;
    private static boolean selectableLookedUp;

    private ExtraRaces() {}

    /**
     * Attribute of an extra race in its current racial form, before the custom
     * form multipliers. {@code neutralizeBase} sets the base form multiplier to
     * 1, like replaceOldMulti does for the original races when the custom form
     * is not vanilla stackable.
     */
    public static int getAttribute(EntityPlayer player, int[] currAttributes, int attribute, int state, int race,
                                   int skillX, int mysticLvl, int powerType, boolean neutralizeBase) {
        MethodHandle h = handle();
        if (h != null) {
            try {
                return (int) h.invokeExact(player, currAttributes, attribute, state, race, skillX, mysticLvl, powerType,
                    neutralizeBase);
            } catch (Throwable t) {
                if (!failed) {
                    failed = true;
                    LOG.error("DbrRazas attribute call failed, using the base attribute from now on", t);
                }
                attributeHandle = null;
            }
        }
        return currAttributes[attribute];
    }

    private static MethodHandle handle() {
        if (!lookedUp) {
            lookedUp = true;
            try {
                Class<?> api = Class.forName(API);
                attributeHandle = MethodHandles.publicLookup().findStatic(api, "getAttribute", MethodType.methodType(int.class,
                    EntityPlayer.class, int[].class, int.class, int.class, int.class, int.class, int.class, int.class,
                    boolean.class));
            } catch (ClassNotFoundException e) {
                LOG.info("DbrRazas not present: extra races use their base attribute in custom forms");
            } catch (ReflectiveOperationException e) {
                LOG.error("DbrRazas present but its RazaApi does not match this addon", e);
            }
        }
        return attributeHandle;
    }

    /**
     * Same rule as the X form selector of DbrRazas: racial level reached and,
     * for the God form, the God skill learned. {@code racialLevel} is the raw
     * SklLvlX value (not minus one). Without DbrRazas nothing is selectable.
     */
    public static boolean isFormSelectable(int race, int form, int racialLevel, int godSkillLevel) {
        if (!selectableLookedUp) {
            selectableLookedUp = true;
            try {
                Class<?> logic = Class.forName(LOGIC);
                selectableHandle = MethodHandles.publicLookup().findStatic(logic, "selectable",
                    MethodType.methodType(boolean.class, int.class, int.class, int.class, int.class));
            } catch (ClassNotFoundException e) {
                LOG.info("DbrRazas not present: extra races have no DBC forms in the form wheel");
            } catch (ReflectiveOperationException e) {
                LOG.error("DbrRazas present but its ExtraRaceLogic.selectable does not match this addon", e);
            }
        }
        if (selectableHandle == null) {
            return false;
        }
        try {
            return (boolean) selectableHandle.invokeExact(race, form, racialLevel, godSkillLevel);
        } catch (Throwable t) {
            LOG.error("DbrRazas selectable call failed, extra race forms disabled in the form wheel", t);
            selectableHandle = null;
            return false;
        }
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
