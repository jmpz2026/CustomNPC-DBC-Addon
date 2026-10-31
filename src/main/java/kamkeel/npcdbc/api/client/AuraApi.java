package kamkeel.npcdbc.api.client;

import JinRyuu.DragonBC.common.Npcs.EntityAura2;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.npcdbc.client.ClientEventHandler;
import kamkeel.npcdbc.controllers.AuraController;
import kamkeel.npcdbc.data.aura.Aura;
import kamkeel.npcdbc.mixins.late.IEntityAura;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Stable entry point for other mods that want a custom aura on any client entity, independent
 * of its DBC data: a cinematic actor (a client-only player copy), or a real player during a
 * staged transformation. Meant to be called by reflection, like {@link BloomApi}; add, never
 * rename.
 *
 * <pre>
 *   AuraApi.start(actor, 64, 0.5f, 0.4f, false); // aura 64 at half size, faint
 *   ...
 *   AuraApi.stop(actor);                         // the live auras fade out on their own
 * </pre>
 *
 * The aura is the DBC one ({@link EntityAura2}) with the custom aura's look (textures, colours,
 * lightning; no aura sound), spawned every 5 ticks while asked for, as DBC does when charging. It is
 * drawn even while a cinematic has the 3D auras turned off, and never as a first-person aura.
 */
@SideOnly(Side.CLIENT)
public final class AuraApi {

    /** Size factor of DBC for a fully released form (state 2 + release 100 * 0.03). */
    private static final float POWER = 5f;

    private static final class Request {
        int auraId;
        float scale, alpha;
        boolean lightning;
    }

    private static final Map<Entity, Request> REQUESTS = new WeakHashMap<Entity, Request>();
    private static int ticks;

    private AuraApi() {}

    /** Whether a custom aura with this id is known on this client. */
    public static boolean exists(int auraId) {
        return AuraController.getInstance().get(auraId) instanceof Aura;
    }

    /**
     * Start, or change, the aura of {@code entity}. {@code scale} multiplies the aura size and
     * {@code alpha} its opacity (1 = as configured). Returns false if the aura id is unknown.
     */
    public static boolean start(Entity entity, int auraId, float scale, float alpha, boolean lightning) {
        if (entity == null || !exists(auraId)) return false;
        Request r = REQUESTS.get(entity);
        if (r == null) {
            r = new Request();
            REQUESTS.put(entity, r);
        }
        r.auraId = auraId;
        r.scale = scale;
        r.alpha = alpha;
        r.lightning = lightning;
        return true;
    }

    /**
     * Same as {@link #start(Entity, int, float, float, boolean)} with the aura's name: ids are given
     * per world, so data shipped by other mods names the aura. Returns false if no aura has it.
     */
    public static boolean start(Entity entity, String auraName, float scale, float alpha, boolean lightning) {
        if (auraName == null) return false;
        Aura aura = AuraController.getInstance().getAuraFromName(auraName);
        return aura != null && start(entity, aura.id, scale, alpha, lightning);
    }

    /** Whether {@code aura} is an aura entity started here (for code that hides the other auras). */
    public static boolean isApiAura(Entity aura) {
        return aura instanceof IEntityAura && ((IEntityAura) aura).getApiPower() >= 0;
    }

    /** No more auras for {@code entity}; the ones alive end their short life as usual. */
    public static void stop(Entity entity) {
        REQUESTS.remove(entity);
    }

    /** Stop every aura started here. */
    public static void stopAll() {
        REQUESTS.clear();
    }

    /** The entity an aura entity belongs to, or null if it is not an aura. */
    public static Entity owner(Entity aura) {
        return aura instanceof IEntityAura ? ((IEntityAura) aura).getEntity() : null;
    }

    /** Called at the end of every client tick by {@link ClientEventHandler}. */
    public static void tick() {
        if (REQUESTS.isEmpty() || ++ticks % 5 != 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        for (Iterator<Map.Entry<Entity, Request>> it = REQUESTS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Entity, Request> e = it.next();
            Entity entity = e.getKey();
            if (entity.isDead || mc.theWorld == null || entity.worldObj != mc.theWorld) {
                it.remove();
                continue;
            }
            Request r = e.getValue();
            Object aura = AuraController.getInstance().get(r.auraId);
            if (!(aura instanceof Aura)) continue;
            EntityAura2 spawned = ClientEventHandler.spawnAura(entity, (Aura) aura, false);
            IEntityAura api = (IEntityAura) spawned;
            api.setApiPower(POWER);
            api.setSize(api.getSize() * r.scale);
            spawned.setAlp(spawned.getAlp() * r.alpha);
            api.setHasLightning(r.lightning);
        }
    }
}
