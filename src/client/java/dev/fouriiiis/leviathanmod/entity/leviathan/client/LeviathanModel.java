package dev.fouriiiis.leviathanmod.entity.leviathan.client;

import dev.fouriiiis.leviathanmod.LeviathanMod;
import dev.fouriiiis.leviathanmod.entity.leviathan.LeviathanEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * Straight-line model alignment.
 * Applies a +90° yaw correction so GEO (+X forward) matches Minecraft (+Z forward).
 */
public class LeviathanModel extends GeoModel<LeviathanEntity> {
    // GEO(+X) -> MC(+Z): rotate +90° around Y (flip from previous -90°).
    private static final float YAW_CORRECTION_RAD = (float) (Math.PI / 2.0);

    @Override
    public Identifier getModelResource(LeviathanEntity animatable) {
        return Identifier.of(LeviathanMod.MOD_ID, "geo/rwmc_leviathan.geo.json");
    }

    @Override
    public Identifier getTextureResource(LeviathanEntity animatable) {
        return Identifier.of(LeviathanMod.MOD_ID, "textures/entity/texture_green.png");
    }

    @Override
    public Identifier getAnimationResource(LeviathanEntity animatable) {
        return Identifier.of(LeviathanMod.MOD_ID, "animations/rwmc_leviathan.animation.json");
    }

    @Override
    public void setCustomAnimations(LeviathanEntity e, long instanceId, AnimationState<LeviathanEntity> state) {
        super.setCustomAnimations(e, instanceId, state);

        // Apply the frame correction on the root
        var root = this.getAnimationProcessor().getBone("root");
        if (root != null) {
            root.setRotX(0f);
            root.setRotY(YAW_CORRECTION_RAD);
            root.setRotZ(0f);
        }

        // Keep all seg bones rotation-zero (straight baseline)
        for (int i = 1; i <= 18; i++) {
            var bone = this.getAnimationProcessor().getBone("seg" + i);
            if (bone != null) {
                bone.setRotX(0f);
                bone.setRotY(0f);
                bone.setRotZ(0f);
            }
        }
    }
}
