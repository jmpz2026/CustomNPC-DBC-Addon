package kamkeel.npcdbc.api.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Stable entry point for other mods that want to hide the native DBC tail of a client entity
 * (the Saiyan one, wrapped or hanging, and the Arcosian one; the Arcosian horns and spikes stay),
 * independent of its form: a cinematic actor that will get a 3D tail before the form is applied,
 * or a real player wearing a custom look. A form can also hide it on its own with
 * {@code display.hideTail}. Meant to be called by reflection, like {@link AuraApi}; add, never
 * rename.
 *
 * <pre>
 *   TailApi.hide(actor);                      // until show(actor) or the entity is gone
 *   TailApi.addRule(e -> myLook(e).noTail);   // asked every time a tail is about to be drawn
 * </pre>
 */
@SideOnly(Side.CLIENT)
public final class TailApi {

    private static final Map<Entity, Boolean> HIDDEN = new WeakHashMap<Entity, Boolean>();
    private static final List<Predicate<Entity>> RULES = new CopyOnWriteArrayList<Predicate<Entity>>();

    private TailApi() {}

    /** Hide the native tail of {@code entity} until {@link #show(Entity)}. */
    public static void hide(Entity entity) {
        if (entity != null) HIDDEN.put(entity, Boolean.TRUE);
    }

    /** Undo {@link #hide(Entity)}; rules and the form can still hide it. */
    public static void show(Entity entity) {
        HIDDEN.remove(entity);
    }

    /** Undo every {@link #hide(Entity)}. */
    public static void showAll() {
        HIDDEN.clear();
    }

    /**
     * Add a rule asked on the render thread each time a native tail is about to be drawn: true
     * hides it. Keep it cheap. A rule that throws is dropped.
     */
    public static void addRule(Predicate<Entity> rule) {
        if (rule != null) RULES.add(rule);
    }

    /** Whether something asked here to hide the tail of {@code entity} (the form is checked apart). */
    public static boolean isHidden(Entity entity) {
        if (entity == null) return false;
        if (HIDDEN.containsKey(entity)) return true;
        for (Predicate<Entity> rule : RULES) {
            try {
                if (rule.test(entity)) return true;
            } catch (Throwable t) {
                RULES.remove(rule);
            }
        }
        return false;
    }
}
