package net.sn0wix_.projectdragons.entity.custom;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ShellSmasherEntity extends TameableEntity implements GeoEntity, VariantHolder<ShellSmasherVariants>, Saddleable {

    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(ShellSmasherEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> HAS_SADDLE = DataTracker.registerData(ShellSmasherEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private boolean isFlying = false;
    private int standUpMovementLockTicks = 0;

    // Maximum yaw change per tick (degrees) when syncing to rider
    private float maxYawChange = 3.0f;

    RawAnimation WALK = RawAnimation.begin().then("move.walk", Animation.LoopType.LOOP);
    RawAnimation FLY = RawAnimation.begin().then("move.fly", Animation.LoopType.LOOP);
    RawAnimation IDLE = RawAnimation.begin().then("move.idle", Animation.LoopType.LOOP);
    RawAnimation SIT = RawAnimation.begin().then("sleep.enter", Animation.LoopType.PLAY_ONCE);
    RawAnimation SITTING = RawAnimation.begin().then("sleep.idle", Animation.LoopType.LOOP);
    RawAnimation STAND_UP = RawAnimation.begin().then("sleep.wake_up", Animation.LoopType.PLAY_ONCE);

    AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    AnimationController<ShellSmasherEntity> genericController = new AnimationController<>(this, "generic", 3, (handler -> {
        if (this.isOnGround()) {
            if (this.isSitting()) {
                return handler.setAndContinue(SITTING);
            }

            if (handler.isMoving()) {
                // TODO reverse walking anim
                return handler.setAndContinue(WALK);
            }

            return handler.setAndContinue(IDLE);
        } else {
            return handler.setAndContinue(FLY);
        }
    })).triggerableAnim("sit", SIT).triggerableAnim("stand_up", STAND_UP);


    public ShellSmasherEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.ignoreCameraFrustum = true;
        this.setMovementSpeed((float) this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) / 4); //?
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new SitGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2f)
                .add(EntityAttributes.GENERIC_SAFE_FALL_DISTANCE, 6)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9f);
    }

    public void toggleSitting() {
        if (this.isSitting()) {
            stopSitting();
        } else {
            startSitting();
        }
    }

    public void startSitting() {
        if (this.isSitting()) return;

        this.playSound(SoundEvents.ENTITY_CAMEL_SIT);
        setInSittingPose(true);
        setSitting(true);

        this.setPose(EntityPose.SITTING);
        genericController.tryTriggerAnimation("sit");
        this.emitGameEvent(GameEvent.ENTITY_ACTION);

        this.navigation.stop();
    }

    public void stopSitting() {
        if (!this.isSitting()) return;

        this.playSound(SoundEvents.ENTITY_CAMEL_STAND);
        setInSittingPose(false);
        setSitting(false);

        this.setPose(EntityPose.STANDING);
        genericController.tryTriggerAnimation("stand_up");
        this.emitGameEvent(GameEvent.ENTITY_ACTION);

        standUpMovementLockTicks = 40;
        this.navigation.stop();
    }

    public boolean isSitting() {
        return this.isInSittingPose();
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        // Taming
        ItemStack itemstack = player.getStackInHand(hand);
        Item item = itemstack.getItem();
        Item itemForTaming = Items.SEAGRASS;

        if (item == itemForTaming && !isTamed()) {
            if (this.getWorld().isClient()) {
                return ActionResult.CONSUME;
            } else {
                if (!player.getAbilities().creativeMode) {
                    itemstack.decrement(1);
                }
                super.setOwner(player);
                this.navigation.recalculatePath();
                this.setTarget(null);
                this.getWorld().sendEntityStatus(this, (byte) 7);
                return ActionResult.SUCCESS;
            }
        }

        // sitting toggle
        if (isTamed() && hand == Hand.MAIN_HAND && player.isSneaking() && !isBreedingItem(itemstack)) {
            toggleSitting();
            return ActionResult.SUCCESS;
        }

        // start riding
        if (!this.isBreedingItem(player.getStackInHand(hand)) && this.isSaddled() && !this.hasPassengers() && !player.shouldCancelInteraction()) {
            if (!this.getWorld().isClient) {
                player.setYaw(this.getYaw());
                //player.setBodyYaw(this.getBodyYaw());
                //player.setHeadYaw(this.getHeadYaw());
                player.startRiding(this);
            }
            return ActionResult.success(this.getWorld().isClient);
        }

        return super.interactMob(player, hand);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient()) {
            if (standUpMovementLockTicks > 0) {
                standUpMovementLockTicks--;

                this.navigation.stop();
                this.velocityDirty = true;
                this.setJumping(false);
            }
        }
    }

    public boolean isMovementLocked() {
        return standUpMovementLockTicks > 0 || this.isSitting();
    }

    // Riding control
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.hasPassengers() && passenger instanceof LivingEntity && super.canAddPassenger(passenger);
    }

    public boolean canBeControlledByRider() {
        return this.isSaddled() && this.getControllingPassenger() instanceof PlayerEntity;
    }

    // Limit how quickly we rotate towards the rider's yaw
    private void applyLimitedYawTowards(float targetYaw) {
        float current = this.getYaw();
        float delta = MathHelper.wrapDegrees(targetYaw - current);
        float clamped = MathHelper.clamp(delta, -maxYawChange, maxYawChange);
        float newYaw = current + clamped;

        this.prevYaw = this.getYaw();
        this.setYaw(newYaw);
        this.setHeadYaw(newYaw);
        this.bodyYaw = newYaw;
    }

    public boolean isFlying() {
        return isFlying;
    }

    public void setFlying(boolean flying) {
        isFlying = flying;
    }

    public void setMaxYawChange(float degreesPerTick) {
        this.maxYawChange = Math.max(0.0f, degreesPerTick);
    }

    public float getMaxYawChange() {
        return this.maxYawChange;
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (this.isMovementLocked()) {
            super.travel(Vec3d.ZERO);
            return;
        }

        LivingEntity controller = this.getControllingPassenger();
        if (this.canBeControlledByRider() && controller != null) {
            // Smoothly align with rider orientation using maxYawChange
            applyLimitedYawTowards(controller.getYaw());
            // Keep some pitch responsiveness but do not affect collision too much
            this.setPitch(controller.getPitch() * 0.5f);

            // Read rider inputs (forward/strafe)
            float forward = 0.0f;

            if (controller instanceof PlayerEntity player) {
                forward = player.forwardSpeed;
            }

            // backwords speed tweaks
            if (forward < 0) {
                forward *= 0.5f;
            }

            // Move
            Vec3d input = new Vec3d(0, 0.0, forward);
            super.travel(input);
            return;
        }

        super.travel(movementInput);
    }

    public void takeOff() {

    }

    @Override
    protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return super.getPassengerAttachmentPos(passenger, dimensions, scaleFactor).add(0, 0.05, 0);
    }

    // Dismounting: try multiple offsets around the entity for a safer exit
    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        // Primary: right side relative to main arm
        Vec3d right = AbstractHorseEntity.getPassengerDismountOffset(
                this.getWidth(), passenger.getWidth(),
                this.getYaw() + (passenger.getMainArm() == Arm.RIGHT ? 90.0f : -90.0f)
        );

        Vec3d pos = this.locateSafeDismountingPos(right, passenger);
        if (pos != null) return pos;

        // Secondary: opposite side
        Vec3d left = AbstractHorseEntity.getPassengerDismountOffset(
                this.getWidth(), passenger.getWidth(),
                this.getYaw() + (passenger.getMainArm() == Arm.LEFT ? 90.0f : -90.0f)
        );
        pos = this.locateSafeDismountingPos(left, passenger);
        if (pos != null) return pos;

        // Additional candidates: forward, back, and diagonals around the mount
        float yawRad = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double cos = MathHelper.cos(yawRad);
        double sin = MathHelper.sin(yawRad);

        Vec3d[] candidates = new Vec3d[]{
                new Vec3d(0.0, 0.0, 1.0),
                new Vec3d(0.0, 0.0, -1.0),
                new Vec3d(1.0, 0.0, 0.0),
                new Vec3d(-1.0, 0.0, 0.0),
                new Vec3d(1.0, 0.0, 1.0),
                new Vec3d(-1.0, 0.0, 1.0),
                new Vec3d(1.0, 0.0, -1.0),
                new Vec3d(-1.0, 0.0, -1.0)
        };

        double radius = (this.getWidth() * 0.5) + passenger.getWidth() + 0.25;

        for (Vec3d local : candidates) {
            double localX = local.x * radius;
            double localZ = local.z * radius;
            double worldX = localX * cos - localZ * sin;
            double worldZ = localX * sin + localZ * cos;

            Vec3d candidate = new Vec3d(worldX, 0.0, worldZ);
            pos = this.locateSafeDismountingPos(candidate, passenger);
            if (pos != null) return pos;
        }

        return this.getPos();
    }

    @Nullable
    private Vec3d locateSafeDismountingPos(Vec3d offset, LivingEntity passenger) {
        double d = this.getX() + offset.x;
        double e = this.getBoundingBox().minY;
        double f = this.getZ() + offset.z;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        block0:
        for (EntityPose entityPose : passenger.getPoses()) {
            mutable.set(d, e, f);
            double maxY = this.getBoundingBox().maxY + 0.75;
            do {
                double dismountHeight = this.getWorld().getDismountHeight(mutable);
                if ((double) mutable.getY() + dismountHeight > maxY) continue block0;
                if (Dismounting.canDismountInBlock(dismountHeight)) {
                    Box box = passenger.getBoundingBox(entityPose);
                    Vec3d place = new Vec3d(d, (double) mutable.getY() + dismountHeight, f);
                    if (Dismounting.canPlaceEntityAt(this.getWorld(), passenger, box.offset(place))) {
                        passenger.setPose(entityPose);
                        return place;
                    }
                }
                mutable.move(Direction.UP);
            } while ((double) mutable.getY() < maxY);
        }
        return null;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        if (this.isSaddled() && this.getFirstPassenger() instanceof PlayerEntity passenger) {
            return passenger;
        }
        return super.getControllingPassenger();
    }

    // Saddle
    @Override
    public boolean canBeSaddled() {
        return this.isTamed();
    }

    @Override
    public void saddle(ItemStack stack, @Nullable SoundCategory soundCategory) {
        this.dataTracker.set(HAS_SADDLE, true);
        if (soundCategory != null) {
            this.getWorld().playSoundFromEntity(null, this, SoundEvents.ENTITY_HORSE_SADDLE, soundCategory, 0.5f, 1.0f);
        }
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.isSaddled()) {
            this.dropItem(Items.SADDLE);
        }
    }

    @Override
    public boolean isSaddled() {
        return this.dataTracker.get(HAS_SADDLE);
    }

    @Override
    public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    // Data saving and syncing
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(VARIANT, 0);
        builder.add(HAS_SADDLE, false);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getEntityVariant());
        nbt.putBoolean("HasSaddle", this.dataTracker.get(HAS_SADDLE));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setEntityVariant(nbt.getInt("Variant"));
        this.dataTracker.set(HAS_SADDLE, nbt.getBoolean("HasSaddle"));
    }

    // Variants
    private void setEntityVariant(int variant) {
        this.dataTracker.set(VARIANT, variant);
    }

    private int getEntityVariant() {
        return this.dataTracker.get(VARIANT);
    }

    @Override
    public ShellSmasherVariants getVariant() {
        return ShellSmasherVariants.byId(this.getEntityVariant());
    }

    @Override
    public void setVariant(ShellSmasherVariants variant) {
        this.setEntityVariant(variant.getId());
    }

    // Geckolib
    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(genericController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
