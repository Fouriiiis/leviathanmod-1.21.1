package dev.fouriiiis.leviathanmod.entity.leviathan;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Baseline entity that swims straight and keeps body perfectly straight.
 * Ensures the model faces the direction of travel.
 */
public class LeviathanEntity extends LivingEntity implements software.bernie.geckolib.animatable.GeoEntity {
    public final LeviathanChain chain;

    // Per-bone rest spacing (blocks)
    private static final double[] REST = new double[]{
        17.0 / 16.0, 67.0 / 16.0, 32.0 / 16.0, 64.0 / 16.0, 79.0 / 16.0,
        86.0 / 16.0, 71.0 / 16.0, 78.0 / 16.0, 78.0 / 16.0, 76.0 / 16.0,
        72.0 / 16.0, 74.0 / 16.0, 65.0 / 16.0, 76.0 / 16.0, 76.0 / 16.0,
        76.0 / 16.0, 76.0 / 16.0
    };
    private static final int SEGMENTS = REST.length + 1;

    // Keep movement constant & simple for this baseline
    private Vec3d swimDir = new Vec3d(1, 0, 0);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LeviathanEntity(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
        chain = new LeviathanChain(SEGMENTS, this.getPos(), REST, swimDir);
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

        // Move straight in swimDir (no steering here for the "perfectly straight" baseline)
        if (!this.getWorld().isClient()) {
            // Vanilla-style yaw so +X is -90°, +Z is 0°, etc.
            float yawDeg = (float) Math.toDegrees(Math.atan2(swimDir.z, swimDir.x)) - 90.0f;

            // Sync all the yaws so renderer + GeckoLib agree
            this.setYaw(yawDeg);
            this.setBodyYaw(yawDeg);
            this.setHeadYaw(yawDeg);
            this.setPitch(0f);

            Vec3d vel = swimDir.multiply(0.06);
            this.setVelocity(vel);
            this.setPos(this.getX() + vel.x, this.getY() + vel.y, this.getZ() + vel.z);
        }

        // Keep the segment anchors perfectly straight behind the head
        chain.update(this.getPos(), swimDir);
    }

    // --- GeoEntity boilerplate ---
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(ControllerRegistrar controllers) { }

    // --- NBT ---
    @Override public void writeCustomDataToNbt(NbtCompound nbt) { }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) { }

    // --- Equipment/misc (unused) ---
    @Override public void equipStack(EquipmentSlot slot, ItemStack stack) { }
    @Override public Iterable<ItemStack> getArmorItems() { return java.util.List.of(); }
    @Override public ItemStack getEquippedStack(EquipmentSlot slot) { return ItemStack.EMPTY; }
    @Override public Arm getMainArm() { return Arm.RIGHT; }
}
