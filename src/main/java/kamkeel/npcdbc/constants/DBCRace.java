package kamkeel.npcdbc.constants;

public class DBCRace {
    public static final int ALL = -1;
    public static final int HUMAN = 0;
    public static final int ALL_SAIYANS = 12;
    public static final int SAIYAN = 1;
    public static final int HALFSAIYAN = 2;
    public static final int NAMEKIAN = 3;
    public static final int ARCOSIAN = 4;
    public static final int MAJIN = 5;
    /** Primera raza extra (DbrRazas). Las razas extra van de 6 a JRMCoreH.Races.length - 1. */
    public static final int KAIOSHIN = 6;

    /** Raza jugable real: 0..5 y las que DbrRazas anade a las tablas de JRMCore. */
    public static boolean isValidRace(int race) {
        return race >= 0 && race < JinRyuu.JRMCore.JRMCoreH.Races.length;
    }

    /** Raza anadida por DbrRazas (no existe en el JRMCore original). */
    public static boolean isExtraRace(int race) {
        return race > MAJIN && isValidRace(race);
    }

    public static boolean isSaiyan(int race) {
        return race == SAIYAN || race == HALFSAIYAN || race == ALL_SAIYANS;
    }
}
