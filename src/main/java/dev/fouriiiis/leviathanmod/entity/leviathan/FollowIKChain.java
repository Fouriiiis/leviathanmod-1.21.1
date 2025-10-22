package dev.fouriiiis.leviathanmod.entity.leviathan;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public final class FollowIKChain {
    private final int n;                 // joint count
    private final Vec3d[] joints;        // 0..n-1
    private final Vec3d[] prevJoints;
    private final double[] lengthsExt;   // link count = n-1

    private boolean initialized;
    private boolean avoidTerrain = true;
    private double tipInertia = 0.12;
    private static final double EPS = 1e-12;

    // Per-link absolute angles (for seg mapping)
    // link k is direction joints[k] -> joints[k+1]
    public final float[] linkYawAbs;   // size = n-1
    public final float[] linkPitchAbs; // size = n-1

    public FollowIKChain(double[] linkLengths) {
        // Extend by one extra link (repeat last length) to get an extra tail joint
        this.lengthsExt = new double[linkLengths.length + 1];
        System.arraycopy(linkLengths, 0, this.lengthsExt, 0, linkLengths.length);
        this.lengthsExt[this.lengthsExt.length - 1] = linkLengths[linkLengths.length - 1];

        this.n = Math.max(2, this.lengthsExt.length + 1);
        this.joints = new Vec3d[n];
        this.prevJoints = new Vec3d[n];

        this.linkYawAbs = new float[n - 1];
        this.linkPitchAbs = new float[n - 1];
    }

    private void lazyInit(Vec3d headPos, Vec3d headForward) {
        if (initialized) return;
        Vec3d dir = safeNorm(headForward);
        joints[0] = headPos;
        prevJoints[0] = headPos;

        double accum = 0.0;
        for (int i = 1; i < n; i++) {
            accum += lengthsExt[i - 1]; // note: i-1 valid up to n-2
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
                joints[i] = a.add(dir.multiply(lengthsExt[i - 1]));
                if (avoidTerrain) joints[i] = pushOutOfSolid(world, joints[i]);
            }
            joints[0] = headPos; // re-pin head
        }

        for (int i = 0; i < n; i++) prevJoints[i] = joints[i];

        computeLinkAngles(headForward);
    }

    // Compute absolute angles for link directions joints[k] -> joints[k+1]
    private void computeLinkAngles(Vec3d headForward) {
        for (int k = 0; k < n - 1; k++) {
            Vec3d d = dirSafe(k, k + 1, headForward);
            linkYawAbs[k]   = yawOf(d);
            linkPitchAbs[k] = pitchOf(d);
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

    private static float yawOf(Vec3d d) { return (float) Math.atan2(d.z, d.x); }
    private static float pitchOf(Vec3d d) {
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        return (float) Math.atan2(d.y, h);
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
    public int jointCount() { return n; }        // joints 0..n-1
    public int linkCount()  { return n - 1; }    // links  0..n-2 (== SEGMENTS)
    public Vec3d joint(int i) { return joints[i]; }
}
