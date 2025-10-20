// Replace your FollowIKChain with this version (only the angle section changed)
package dev.fouriiiis.leviathanmod.entity.leviathan;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public final class FollowIKChain {
    private final int n;
    private final Vec3d[] joints;
    private final Vec3d[] prevJoints;
    private final double[] lengths;

    private boolean initialized;
    private boolean avoidTerrain = true;
    private double tipInertia = 0.12;
    private static final double EPS = 1e-12;

    // Existing outputs (link-to-link relative)
    public final float[] yawRel;
    public final float[] pitchRel;

    // Absolute yaw/pitch per link direction (i = 1..n-1)
    public final float[] yawAbs;   // absolute yaw per link direction
    public final float[] pitchAbs; // absolute pitch per link direction

    public FollowIKChain(double[] linkLengths) {
        this.n = Math.max(2, linkLengths.length + 1);
        this.joints = new Vec3d[n];
        this.prevJoints = new Vec3d[n];
        this.lengths = linkLengths.clone();

    this.yawRel = new float[n];
    this.pitchRel = new float[n];
    this.yawAbs = new float[n];
    this.pitchAbs = new float[n];
    }

    private void lazyInit(Vec3d headPos, Vec3d headForward) {
        if (initialized) return;
        Vec3d dir = safeNorm(headForward);
        joints[0] = headPos;
        prevJoints[0] = headPos;

        double accum = 0.0;
        for (int i = 1; i < n; i++) {
            accum += lengths[i - 1];
            Vec3d p = headPos.subtract(dir.multiply(accum));
            joints[i] = p;
            prevJoints[i] = p;
        }
        initialized = true;
    }

    public void solveFollow(World world, Vec3d headPos, Vec3d headForward, int iterations) {
        lazyInit(headPos, headForward);

        joints[0] = headPos;

        if (tipInertia > 0.0) {
            int tip = n - 1;
            Vec3d tipPrev = prevJoints[tip];
            joints[tip] = joints[tip].multiply(1.0 - tipInertia).add(tipPrev.multiply(tipInertia));
        }

        int iters = Math.max(1, iterations);
        for (int it = 0; it < iters; it++) {
            for (int i = 1; i < n; i++) {
                Vec3d a = joints[i - 1];
                Vec3d b = joints[i];
                Vec3d d = b.subtract(a);
                double l2 = d.lengthSquared();
                Vec3d dir = (l2 < EPS) ? safeNorm(headForward) : d.multiply(1.0 / Math.sqrt(l2));
                joints[i] = a.add(dir.multiply(lengths[i - 1]));
                if (avoidTerrain) joints[i] = pushOutOfSolid(world, joints[i]);
            }
            joints[0] = headPos;
        }

        for (int i = 0; i < n; i++) prevJoints[i] = joints[i];

        computeRelativeAngles(headForward);
    }

    // -------- ANGLES --------
    private void computeRelativeAngles(Vec3d headForward) {
        // Absolute yaw/pitch for each link (direction joints[i]-joints[i-1])
        Vec3d d01 = dirSafe(0, 1, headForward);
        yawAbs[1]   = yawOf(d01);
        pitchAbs[1] = pitchOf(d01);

        for (int i = 2; i < n; i++) {
            Vec3d d = dirSafe(i - 1, i, headForward);
            yawAbs[i]   = yawOf(d);
            pitchAbs[i] = pitchOf(d);
        }

        // Link-to-link RELATIVE (for hierarchical chains like your GEO)
        yawRel[1] = 0f;
        pitchRel[1] = 0f;
        for (int i = 2; i < n; i++) {
            yawRel[i]   = wrap(yawAbs[i]   - yawAbs[i - 1]);
            pitchRel[i] = wrap(pitchAbs[i] - pitchAbs[i - 1]);
        }
    }

    private Vec3d dirSafe(int ia, int ib, Vec3d fallback) {
        Vec3d d = joints[ib].subtract(joints[ia]);
        double l2 = d.lengthSquared();
        if (l2 < EPS) return safeNorm(fallback);
        return d.multiply(1.0 / Math.sqrt(l2));
    }

    private static Vec3d safeNorm(Vec3d v) {
        double l2 = v.lengthSquared();
        if (l2 < EPS) return new Vec3d(1, 0, 0);
        return v.multiply(1.0 / Math.sqrt(l2));
    }

    private static float yawOf(Vec3d d) {
        return (float) Math.atan2(d.z, d.x);
    }
    private static float pitchOf(Vec3d d) {
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        return (float) Math.atan2(d.y, h);
    }
    private static float wrap(float a) {
        while (a <= -Math.PI) a += (float) (2 * Math.PI);
        while (a >  Math.PI) a -= (float) (2 * Math.PI);
        return a;
    }

    private static Vec3d pushOutOfSolid(World world, Vec3d p) {
        BlockPos bp = BlockPos.ofFloored(p);
        if (!world.getBlockState(bp).isSolidBlock(world, bp)) return p;
        for (Direction d : Direction.values()) {
            BlockPos nb = bp.offset(d);
            if (!world.getBlockState(nb).isSolidBlock(world, nb)) {
                Vec3i off = d.getVector();
                return Vec3d.ofCenter(nb).subtract(off.getX() * 0.49, off.getY() * 0.49, off.getZ() * 0.49);
            }
        }
        return p;
    }

    // Tweaks
    public void setTipInertia(double t) { tipInertia = Math.max(0.0, Math.min(0.5, t)); }
    public void setAvoidTerrain(boolean v) { avoidTerrain = v; }

    // Access
    public int size() { return n; }
    public Vec3d joint(int i) { return joints[i]; }
}
