package kamkeel.npcdbc.client.gui.dbc;

import JinRyuu.JRMCore.JRMCoreH;
import net.minecraft.entity.Entity;

import java.util.Map;

/**
 * DBR attribute rules (WIL on the physical hit, Balance, drain, DEX mitigation, control resistance,
 * blocking), only to show them on the sheet. The server applies them (DbrTalentosMod, numbers in the
 * DbrTalentosPlugin config); the Talentos client sends the numbers here with {@link #fijar}, by
 * reflection, so the sheet shows the same values the server uses.
 */
public final class StatsDbrClient {

    public static volatile boolean recibido;
    public static volatile double wilGolpe = 0.5;
    public static volatile double equiMin = 0.2, equiMax = 0.6, equiCaida = 0.4, equiBono = 0.17;
    public static volatile double drenajeMax = 0.40;
    public static volatile double dexMitMax = 0.35, dexK = 1.0;
    public static volatile double controlMax = 0.50;
    public static volatile double bloqueoMax = 0.50;

    private StatsDbrClient() {}

    /** Called by the Talentos client mod. Missing keys keep their value. */
    public static void fijar(Map<String, Double> m) {
        if (m == null) return;
        wilGolpe = get(m, "wil.golpe", wilGolpe);
        equiMin = get(m, "equilibrio.min", equiMin);
        equiMax = get(m, "equilibrio.max", equiMax);
        equiCaida = get(m, "equilibrio.caida", equiCaida);
        equiBono = get(m, "equilibrio.bono", equiBono);
        drenajeMax = get(m, "wil.drenaje", drenajeMax);
        dexMitMax = get(m, "dex.mitigacion", dexMitMax);
        dexK = get(m, "dex.k", dexK);
        controlMax = get(m, "dex.control", controlMax);
        bloqueoMax = get(m, "dex.bloqueo", bloqueoMax);
        recibido = true;
    }

    private static double get(Map<String, Double> m, String k, double def) {
        Double v = m.get(k);
        return v == null ? def : v;
    }

    /** Melee stat that WIL adds to every physical hit (before release), like STR of this race and class. */
    public static int wilAlGolpe(int wilConForma, int powerType, int race, int classID) {
        if (wilGolpe <= 0 || wilConForma <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, wilGolpe * JRMCoreH.stat((Entity) null, 0, powerType, 0, wilConForma, race, classID, 0f));
    }

    /** Balance bonus (fraction) for WIL/STR. */
    public static double equilibrio(double wil, double str) {
        if (equiBono <= 0 || str <= 0) return 0d;
        double r = wil / str;
        if (r < equiMin) return equiMin <= 0 ? equiBono : equiBono * r / equiMin;
        if (r <= equiMax) return equiBono;
        return equiCaida <= 0 ? 0d : equiBono * Math.max(0d, 1d - (r - equiMax) / equiCaida);
    }

    /** Kaioken and form drain reduction (fraction) from raw WIL and STR. */
    public static double drenaje(double wil, double str) {
        if (drenajeMax <= 0 || wil + str <= 0) return 0d;
        return Math.min(drenajeMax, drenajeMax * 2d * wil / (wil + str));
    }

    /** DEX mitigation (fraction) of a hit {@code golpe} against defense {@code defensa}. */
    public static double mitigacion(double defensa, double golpe) {
        if (dexMitMax <= 0 || defensa <= 0) return 0d;
        return dexMitMax * defensa / (defensa + dexK * Math.max(1d, golpe));
    }

    /** Block stamina saving (fraction), same curve as the mitigation. */
    public static double bloqueo(double defensa, double golpe) {
        if (bloqueoMax <= 0 || defensa <= 0) return 0d;
        return bloqueoMax * defensa / (defensa + dexK * Math.max(1d, golpe));
    }

    /** Control (stun, hold, pull) duration reduction against an attacker with STR {@code str}. */
    public static double control(double dex, double str) {
        if (controlMax <= 0 || dex <= 0) return 0d;
        return controlMax * dex / (dex + Math.max(1d, str));
    }
}
