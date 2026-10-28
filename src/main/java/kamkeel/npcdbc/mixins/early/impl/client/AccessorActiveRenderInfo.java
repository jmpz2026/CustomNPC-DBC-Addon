package kamkeel.npcdbc.mixins.early.impl.client;

import net.minecraft.client.renderer.ActiveRenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.FloatBuffer;

/** The camera matrices {@code ActiveRenderInfo.updateRenderInfo} already read from GL this frame. */
@Mixin(ActiveRenderInfo.class)
public interface AccessorActiveRenderInfo {

    @Accessor("modelview")
    static FloatBuffer npcdbc$getModelview() {
        throw new AssertionError();
    }

    @Accessor("projection")
    static FloatBuffer npcdbc$getProjection() {
        throw new AssertionError();
    }
}
