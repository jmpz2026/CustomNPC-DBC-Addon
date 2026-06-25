package kamkeel.npcdbc.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

/**
 * Distance-based level-of-detail helpers for the DBC render path.
 * <p>
 * Far-away entities cover few pixels, so expensive per-entity detail (the multi
 * draw-call face, extra aura layers) can be skipped or thinned with no perceptible
 * difference while cutting draw calls for crowds. Client-thread only.
 */
public final class RenderLOD {

    private RenderLOD() {
    }

    /**
     * True if {@code entity} is farther than {@code maxBlocks} from the camera.
     * {@code maxBlocks <= 0} disables the check (LOD off, always returns false).
     */
    public static boolean beyond(Entity entity, int maxBlocks) {
        if (maxBlocks <= 0 || entity == null)
            return false;
        Entity viewer = Minecraft.getMinecraft().renderViewEntity;
        if (viewer == null)
            return false;
        return entity.getDistanceSqToEntity(viewer) > (double) maxBlocks * maxBlocks;
    }
}
