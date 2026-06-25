package kamkeel.npcdbc.client.utils;

import net.minecraft.util.ResourceLocation;

import java.util.HashMap;

/**
 * Caches {@link ResourceLocation} instances by their string form.
 * <p>
 * Render paths build the same texture paths every frame via string concat and
 * {@code new ResourceLocation(...)}, which re-parses/lowercases/allocs each time.
 * ResourceLocation is immutable, so caching is 100% correct and avoids the churn.
 * Client-thread only; no synchronization needed.
 */
public final class RLCache {

    private static final HashMap<String, ResourceLocation> CACHE = new HashMap<String, ResourceLocation>();

    private RLCache() {
    }

    /** Cached equivalent of {@code new ResourceLocation(path)}. */
    public static ResourceLocation get(String path) {
        ResourceLocation rl = CACHE.get(path);
        if (rl == null) {
            rl = new ResourceLocation(path);
            CACHE.put(path, rl);
        }
        return rl;
    }

    /** Cached equivalent of {@code new ResourceLocation(domain, path)}. */
    public static ResourceLocation get(String domain, String path) {
        String key = domain + ':' + path;
        ResourceLocation rl = CACHE.get(key);
        if (rl == null) {
            rl = new ResourceLocation(domain, path);
            CACHE.put(key, rl);
        }
        return rl;
    }
}
