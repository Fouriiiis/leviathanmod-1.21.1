package dev.fouriiiis.leviathanmod.entity.leviathan.client;

import dev.fouriiiis.leviathanmod.entity.leviathan.LeviathanEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LeviathanRenderer extends GeoEntityRenderer<LeviathanEntity> {
    public LeviathanRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new LeviathanModel());
        this.shadowRadius = 0.7f;
    }

    // Let bones handle yaw/pitch from the chain; don't spin the whole model again.
    @Override
    protected void applyRotations(LeviathanEntity entity, MatrixStack matrices,
                                  float ageInTicks, float rotationYaw, float partialTick) {
        // no-op
    }
}
