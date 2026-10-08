package kamkeel.npcdbc.client.utils;

import net.minecraft.util.ResourceLocation;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared {@link ResourceLocation} instances for texture paths built while rendering.
 *
 * <p>The render path used to build the same path and a new {@code ResourceLocation} for every body part, every
 * frame and every DBC entity on screen. A {@code ResourceLocation} is immutable and compared by value, so handing
 * out the same instance changes nothing for the texture manager. The paths come from a bounded set (races, forms,
 * body and face types), so the map stays small.
 *
 * <p>Concurrent map: almost every call comes from the render thread, but {@code EntityAura} also builds its
 * textures here and nothing guarantees which thread constructs it.
 */
public final class RLCache {

    private static final ConcurrentHashMap<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();

    private RLCache() {}

    public static ResourceLocation get(String path) {
        ResourceLocation loc = CACHE.get(path);
        if (loc == null) {
            loc = new ResourceLocation(path);
            CACHE.putIfAbsent(path, loc);
        }
        return loc;
    }

    public static ResourceLocation get(String domain, String path) {
        String key = domain + ':' + path;
        ResourceLocation loc = CACHE.get(key);
        if (loc == null) {
            loc = new ResourceLocation(domain, path);
            CACHE.putIfAbsent(key, loc);
        }
        return loc;
    }
}
