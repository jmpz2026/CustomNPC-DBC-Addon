package kamkeel.npcdbc.client;

import JinRyuu.JRMCore.server.config.dbc.JGConfigRaces;
import kamkeel.npcdbc.data.dbcdata.DBCData;
import net.minecraft.entity.player.EntityPlayer;
import noppes.npcs.config.ConfigClient;
import noppes.npcs.util.CacheHashMap;

import java.util.HashMap;

public class ClientCache {

    public static boolean fromRenderPlayerJBRA;
    public static boolean isChangePart;
    public static final CacheHashMap<String, CacheHashMap.CachedObject<DBCData>> clientDataCache = new CacheHashMap<>((long) ConfigClient.CacheLife * 60 * 1000);

    public static boolean allowTransformBypass = false;
    public static boolean hasChargingDex = false;
    public static HashMap<Integer, Float> chargingDexValues = new HashMap<>();

    public static boolean kiRevamp = true;

    public static float divineMulti = 1.0f;
    public static HashMap<Integer, HashMap<String, Boolean>> divineApplicableForms = new HashMap<>();
    public static int maxAbsorptionLevel = JGConfigRaces.CONFIG_MAJIN_ABSORPTON_MAX_LEVEL;

    public static String discordURL = null;

    /** How often {@link #getClientData} runs the expiry sweep of {@link CacheHashMap#get}. */
    private static final long SWEEP_INTERVAL_MS = 1000;
    private static long lastSweep;

    /**
     * Called many times per frame for every rendered player. {@link CacheHashMap#get} walks the whole map to
     * expire old entries on every call, so the lookup goes through {@code getOrDefault} (HashMap's own lookup,
     * not overridden) and the sweep runs at most once per {@link #SWEEP_INTERVAL_MS}. Entries still expire
     * after {@code CacheLife} minutes, at most one interval late.
     */
    public static DBCData getClientData(EntityPlayer player) {
        String name = player.getCommandSenderName();
        synchronized (clientDataCache) {
            CacheHashMap.CachedObject<DBCData> cached = clientDataCache.getOrDefault(name, null);
            if (cached == null) {
                cached = new CacheHashMap.CachedObject<>(new DBCData(player));
                clientDataCache.put(name, cached);
            }
            long now = System.currentTimeMillis();
            if (now - lastSweep >= SWEEP_INTERVAL_MS) {
                lastSweep = now;
                return clientDataCache.get(name).getObject();
            }
            cached.updateTimeAccessed();
            return cached.getObject();
        }
    }
}
