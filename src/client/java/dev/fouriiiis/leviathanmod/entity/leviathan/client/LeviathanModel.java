package dev.fouriiiis.leviathanmod.entity.leviathan.client;

import dev.fouriiiis.leviathanmod.LeviathanMod;
import dev.fouriiiis.leviathanmod.entity.leviathan.LeviathanEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

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

    private static float wrapPi(float a) {
        while (a <= -Math.PI) a += (float)(2*Math.PI);
        while (a >   Math.PI) a -= (float)(2*Math.PI);
        return a;
    }

    @Override
    public void setCustomAnimations(LeviathanEntity e, long instanceId, AnimationState<LeviathanEntity> state) {
        super.setCustomAnimations(e, instanceId, state);

        var root = this.getAnimationProcessor().getBone("root");
        if (root != null) {
            root.setRotX(0f);
            root.setRotY(0f); // GEO +X -> MC +Z
            root.setRotZ(0f);
        }
        if (e == null || e.chain == null) return;

        // Entity facing (instantaneous; no interpolation)
        final float entityYawRad   = (float)Math.toRadians(e.getYaw());
        final float entityPitchRad = (float)Math.toRadians(e.getPitch());

        // seg count = number of bones (seg1..seg18). link count == seg count due to extra tail joint
        final int segCount = Math.min(18, e.chain.linkCount()); // safety vs. geometry
        if (segCount <= 0) return;

        // --- seg1: delta(link0 - entityFacing) ---
        var seg1 = this.getAnimationProcessor().getBone("seg1");
        if (seg1 != null) {
            float link0Yaw   = e.chain.linkYawAbs[0];
            float link0Pitch = e.chain.linkPitchAbs[0];

            float dy = wrapPi(link0Yaw   - entityYawRad);
            float dx = wrapPi(link0Pitch - entityPitchRad);

            // Apply (note: flip signs if your rig bends opposite)
            seg1.setRotY(-dy);
            seg1.setRotX(-dx);
            seg1.setRotZ(0f);
        }

        // --- seg2..segN: delta(link(i-1) - link(i-2)) ---
        for (int i = 2; i <= segCount; i++) {
            var bone = this.getAnimationProcessor().getBone("seg" + i);
            if (bone == null) continue;

            int k  = i - 1; // link index for seg i
            int pk = k - 1; // previous link

            float yawK   = e.chain.linkYawAbs[k];
            float pitchK = e.chain.linkPitchAbs[k];
            float yawP   = e.chain.linkYawAbs[pk];
            float pitchP = e.chain.linkPitchAbs[pk];

            float dy = wrapPi(yawK   - yawP);
            float dx = wrapPi(pitchK - pitchP);

            bone.setRotY(-dy);
            bone.setRotX(-dx);
            bone.setRotZ(0f);
        }

        // If your geometry has more bones than links, zero remaining
        for (int i = segCount + 1; i <= 18; i++) {
            var bone = this.getAnimationProcessor().getBone("seg" + i);
            if (bone != null) { bone.setRotX(0f); bone.setRotY(0f); bone.setRotZ(0f); }
        }
    }
}
