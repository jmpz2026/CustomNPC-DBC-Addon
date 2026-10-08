package kamkeel.npcdbc.client.utils;

import kamkeel.npcdbc.client.ClientConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

/** Distance checks for the detail settings in {@code ConfigDBCClient}. Render thread only. */
public final class RenderLOD {

    private RenderLOD() {}

    /**
     * True when {@code entity} is farther than {@code maxDistance} blocks from the camera. Always false for
     * {@code maxDistance <= 0} (no limit) and while drawing a GUI preview, where the model is close to the eye
     * whatever its position in the world.
     */
    public static boolean beyond(Entity entity, int maxDistance) {
        if (maxDistance <= 0 || entity == null || ClientConstants.renderingGUI)
            return false;
        Entity view = Minecraft.getMinecraft().renderViewEntity;
        if (view == null || view == entity)
            return false;
        double max = maxDistance;
        return view.getDistanceSqToEntity(entity) > max * max;
    }
}
