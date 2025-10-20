package dev.fouriiiis.leviathanmod.entity.leviathan;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Leviathan head entity:
 * - Smoothly homes toward the nearest player (XZ by default).
 * - Drives a follow IK chain (head pinned, tail dragged).
 * - Exposes relative yaw/pitch per link for the model bones.
 */
public class LeviathanEntity extends LivingEntity implements software.bernie.geckolib.animatable.GeoEntity {
    // Per-link rest distances (blocks) — same as your previous REST array
    private static final double[] REST = new double[]{
        17.0/16.0, 67.0/16.0, 32.0/16.0, 64.0/16.0, 79.0/16.0,
        86.0/16.0, 71.0/16.0, 78.0/16.0, 78.0/16.0, 76.0/16.0,
        72.0/16.0, 74.0/16.0, 65.0/16.0, 76.0/16.0, 76.0/16.0,
        76.0/16.0, 76.0/16.0
    };
    private static final int SEGMENTS = REST.length + 1;

    // Motion & steering
    private static final double SPEED_PER_TICK = 0.06; // forward speed (blocks/tick)
    private static final double SEEK_RADIUS = 64.0;    // player search radius
    private static final float  STEER_LERP = 0.12f;    // heading blend per tick [0..1]
    private static final boolean ALLOW_PITCH = false;  // true = 3D pursuit, false = XZ only

    private Vec3d swimDir = new Vec3d(1, 0, 0);

    // IK Chain
    public final FollowIKChain chain;

    // GeckoLib
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LeviathanEntity(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
        this.chain = new FollowIKChain(REST);
        this.chain.setTipInertia(0.14);
        this.noClip = true;
    }

    /** Base attributes */
    public static DefaultAttributeContainer.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient()) {
            // 1) Find nearest player
            PlayerEntity target = this.getWorld().getClosestPlayer(this.getX(), this.getY(), this.getZ(), SEEK_RADIUS, null);

            // 2) Desired heading
            Vec3d desiredDir = swimDir;
            if (target != null) {
                Vec3d to = target.getPos().subtract(this.getPos());
                desiredDir = ALLOW_PITCH ? normalize(to) : normalizedXZ(to);
            }

            // 3) Smooth steering
            swimDir = ALLOW_PITCH
                    ? normalize(lerp(swimDir, desiredDir, STEER_LERP))
                    : normalizedXZ(lerp(swimDir, desiredDir, STEER_LERP));

            // 4) Sync yaw/pitch
            float yawDeg = (float) Math.toDegrees(Math.atan2(swimDir.z, swimDir.x)) - 90.0f;
            this.setYaw(yawDeg);
            this.setBodyYaw(yawDeg);
            this.setHeadYaw(yawDeg);
            this.setPitch(ALLOW_PITCH ? (float) Math.toDegrees(Math.asin(clamp(swimDir.y, -1.0, 1.0))) : 0f);

            // 5) Advance
            Vec3d vel = swimDir.multiply(SPEED_PER_TICK);
            this.setVelocity(vel);
            this.setPos(this.getX() + vel.x, this.getY() + vel.y, this.getZ() + vel.z);
        }

        // 6) Update follow chain (head pinned; tail dragged)
        chain.solveFollow(this.getWorld(), this.getPos(), swimDir, 2); // 1–3 iterations recommended
    }

    // ---------- Helpers ----------
    private static Vec3d lerp(Vec3d a, Vec3d b, float t) {
        double s = 1.0 - t;
        return new Vec3d(a.x * s + b.x * t, a.y * s + b.y * t, a.z * s + b.z * t);
    }
    private static Vec3d normalizedXZ(Vec3d v) {
        double x = v.x, z = v.z;
        double len2 = x * x + z * z;
        if (len2 < 1e-12) return new Vec3d(1, 0, 0);
        double inv = 1.0 / Math.sqrt(len2);
        return new Vec3d(x * inv, 0.0, z * inv);
    }
    private static Vec3d normalize(Vec3d v) {
        double len2 = v.lengthSquared();
        if (len2 < 1e-12) return new Vec3d(1, 0, 0);
        double inv = 1.0 / Math.sqrt(len2);
        return new Vec3d(v.x * inv, v.y * inv, v.z * inv);
    }
    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // ---------- GeoEntity boilerplate ----------
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(ControllerRegistrar controllers) { }

    // ---------- NBT (unused) ----------
    @Override public void writeCustomDataToNbt(NbtCompound nbt) { }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) { }

    // ---------- Equipment/misc (unused) ----------
    @Override public void equipStack(EquipmentSlot slot, ItemStack stack) { }
    @Override public Iterable<ItemStack> getArmorItems() { return java.util.List.of(); }
    @Override public ItemStack getEquippedStack(EquipmentSlot slot) { return ItemStack.EMPTY; }
    @Override public Arm getMainArm() { return Arm.RIGHT; }

    // ---------- Expose relative angles for renderer ----------
    public float getRelYaw(int linkIndex)  { return chain.yawRel[linkIndex]; }
    public float getRelPitch(int linkIndex){ return chain.pitchRel[linkIndex]; }
}
