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
 * Leviathan head entity (snap-to-player, no smoothing):
 * - Instantly orients and moves toward the nearest player (XZ by default).
 * - Drives a follow IK chain with an extra tail joint (SEGMENTS links for SEGMENTS bones).
 * - Disables vanilla render interpolation for yaw/pitch to avoid easing.
 */
public class LeviathanEntity extends LivingEntity implements software.bernie.geckolib.animatable.GeoEntity {
    // Per-link rest distances (blocks) for seg1..seg18
    private static final double[] REST = new double[]{
        17.0/16.0, 67.0/16.0, 32.0/16.0, 64.0/16.0, 79.0/16.0,
        86.0/16.0, 71.0/16.0, 78.0/16.0, 78.0/16.0, 76.0/16.0,
        72.0/16.0, 74.0/16.0, 65.0/16.0, 76.0/16.0, 76.0/16.0,
        76.0/16.0, 76.0/16.0
    };
    public static final int SEGMENTS = REST.length + 1; // seg1..seg18 (18), links = SEGMENTS

    private static final double SPEED_PER_TICK = 0.06;
    private static final double SEEK_RADIUS = 64.0;
    private static final boolean ALLOW_PITCH = false;

    private Vec3d swimDir = new Vec3d(1, 0, 0);

    public final FollowIKChain chain;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LeviathanEntity(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
        this.chain = new FollowIKChain(REST); // chain adds an extra tail point internally
        this.chain.setTipInertia(0.14);
        this.noClip = true;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35);
    }

    @Override
    public void tick() {
        super.tick();

        // Compute heading EVERYWHERE so client has exact facing for hitbox/blue arrow
        PlayerEntity target = this.getWorld().getClosestPlayer(this.getX(), this.getY(), this.getZ(), SEEK_RADIUS, null);
        if (target != null) {
            Vec3d to = target.getPos().subtract(this.getPos());
            swimDir = ALLOW_PITCH ? normalize(to) : normalizedXZ(to);
        }

        // Snap orientation
        float yawDeg = (float) Math.toDegrees(Math.atan2(swimDir.z, swimDir.x)) - 90.0f;
        this.setYaw(yawDeg);
        this.setBodyYaw(yawDeg);
        this.setHeadYaw(yawDeg);
        this.setPitch(ALLOW_PITCH ? (float) Math.toDegrees(Math.asin(clamp(swimDir.y, -1.0, 1.0))) : 0f);

        // Server moves
        if (!this.getWorld().isClient()) {
            Vec3d vel = swimDir.multiply(SPEED_PER_TICK);
            this.setVelocity(vel);
            this.setPos(this.getX() + vel.x, this.getY() + vel.y, this.getZ() + vel.z);
        }

        // Kill interpolation (both sides)
        this.prevYaw = this.getYaw();
        this.prevBodyYaw = this.bodyYaw;
        this.prevHeadYaw = this.headYaw;
        this.prevPitch = this.getPitch();

        // Solve IK after transforms are finalized this tick
        chain.solveFollow(this.getWorld(), this.getPos(), swimDir, 2);
    }

    // Return RAW values (no tickDelta lerp).
    @Override public float getYaw(float tickDelta) { return this.getYaw(); }
    @Override public float getPitch(float tickDelta) { return this.getPitch(); }

    // Helpers
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

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(ControllerRegistrar controllers) { }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) { }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) { }

    @Override public void equipStack(EquipmentSlot slot, ItemStack stack) { }
    @Override public Iterable<ItemStack> getArmorItems() { return java.util.List.of(); }
    @Override public ItemStack getEquippedStack(EquipmentSlot slot) { return ItemStack.EMPTY; }
    @Override public Arm getMainArm() { return Arm.RIGHT; }
}
