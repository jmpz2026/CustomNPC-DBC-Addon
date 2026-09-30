package kamkeel.npcdbc.client.gui.dbc;

import JinRyuu.JRMCore.JRMCoreH;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;

/**
 * DBR "poder total": released power above 100% without touching the
 * {@code jrmcRelease} byte (capped at 127). DbrPoderPlugin stores it in
 * {@code dbrPoderTotal} and DbrMod's coremod appends it to {@code dat10} as a
 * third column ("release;stamina;total").
 *
 * <p>Same rule as {@code dbr.main.coremod.PoderFix#totalCliente} in DbrMod:
 * active when release &gt;= 100 and total &gt; 100. If that rule changes, change
 * it in both places. Without DbrMod the column is missing and this returns 0,
 * so the stat sheet looks exactly as before.</p>
 */
@SideOnly(Side.CLIENT)
public final class PoderTotalClient {

    public static final int BASE = 100;
    /** Physical cap for size and aura, compiled into DbrMod. */
    public static final int TOPE_FISICO = 150;
    private static final int TOPE_DURO = 10000;

    private PoderTotalClient() {
    }

    /** Active total of the local player, or 0. */
    public static int total(int release) {
        if (release < BASE) {
            return 0;
        }
        Minecraft mc = Minecraft.getMinecraft();
        String[] plyrs = JRMCoreH.plyrs;
        String[] dat10 = JRMCoreH.dat10;
        if (mc.thePlayer == null || plyrs == null || dat10 == null) {
            return 0;
        }
        String name = mc.thePlayer.getCommandSenderName();
        int n = Math.min(plyrs.length, dat10.length);
        for (int i = 0; i < n; i++) {
            if (name.equals(plyrs[i])) {
                return parse(dat10[i]);
            }
        }
        return 0;
    }

    private static int parse(String entry) {
        if (entry == null) {
            return 0;
        }
        String[] parts = entry.split(";");
        if (parts.length < 3) {
            return 0;
        }
        try {
            int total = Integer.parseInt(parts[2].trim());
            return total > BASE ? Math.min(total, TOPE_DURO) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
