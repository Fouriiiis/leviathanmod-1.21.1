package dev.fouriiiis.leviathanmod.entity.leviathan.client;

import dev.fouriiiis.leviathanmod.LeviathanMod;
import dev.fouriiiis.leviathanmod.entity.leviathan.LeviathanEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * Applies +90° yaw correction so GEO(+X) faces MC(+Z),
 * then bends each segN bone using RELATIVE yaw/pitch from the follow IK chain.
 */
public class LeviathanModel extends GeoModel<LeviathanEntity> {
    private static final float YAW_CORRECTION_RAD = (float)(Math.PI / 2.0);

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

        // Root correction: GEO +X -> MC +Z
        var root = this.getAnimationProcessor().getBone("root");
        if (root != null) {
            root.setRotX(0f);
            root.setRotY(YAW_CORRECTION_RAD);
            root.setRotZ(0f);
        }

        if (e == null || e.chain == null) return;

        // seg1: entity yaw already points head; only apply absolute pitch from link-1
        var seg1 = this.getAnimationProcessor().getBone("seg1");
        if (seg1 != null && e.chain.size() > 1) {
            seg1.setRotY(0f);
            seg1.setRotX(e.chain.pitchAbs[1]);
            seg1.setRotZ(0f);
        }

        // seg2..seg18: use RELATIVE deltas for hierarchical GEO
        for (int i = 2; i <= 18; i++) {
            var bone = this.getAnimationProcessor().getBone("seg" + i);
            if (bone == null) continue;
            if (i < e.chain.size()) {
                bone.setRotY(e.chain.yawRel[i]);
                bone.setRotX(e.chain.pitchRel[i]);
                bone.setRotZ(0f);
            } else {
                bone.setRotX(0f); bone.setRotY(0f); bone.setRotZ(0f);
            }
        }
    }
}
