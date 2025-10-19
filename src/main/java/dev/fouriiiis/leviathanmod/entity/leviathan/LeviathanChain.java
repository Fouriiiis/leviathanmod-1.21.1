package dev.fouriiiis.leviathanmod.entity.leviathan;

import net.minecraft.util.math.Vec3d;

/**
 * Baseline straight-chain implementation.
 * All segments stay in a straight line behind the head, with no IK bending.
 */
public class LeviathanChain {
    public final int n;         // number of segments
    public final Vec3d[] pos;   // world-space anchors for seg1..segn
    public final double[] rest; // rest distances between consecutive segments (blocks)

    public LeviathanChain(int segments, Vec3d headOrigin, double[] restBlocks, Vec3d forward) {
        this.n = segments;
        this.pos = new Vec3d[n];
        this.rest = restBlocks.clone();

        // seed a perfectly straight line behind head
        Vec3d dir = safeNorm(forward);
        pos[0] = headOrigin;
        double accum = 0.0;
        for (int i = 1; i < n; i++) {
            accum += rest[i - 1];
            pos[i] = headOrigin.subtract(dir.multiply(accum));
        }
    }

    /** Keep a perfectly straight line behind the head */
    public void update(Vec3d headPos, Vec3d headForward) {
        Vec3d dir = safeNorm(headForward);
        pos[0] = headPos;
        double accum = 0.0;
        for (int i = 1; i < n; i++) {
            accum += rest[i - 1];
            pos[i] = headPos.subtract(dir.multiply(accum));
        }
    }

    private static Vec3d safeNorm(Vec3d v) {
        double l2 = v.lengthSquared();
        return l2 < 1.0e-12 ? new Vec3d(1, 0, 0) : v.multiply(1.0 / Math.sqrt(l2));
    }
}
