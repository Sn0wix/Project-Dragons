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

    private int standUpMovementLockTicks = 0;

    RawAnimation WALK = RawAnimation.begin().then("move.walk", Animation.LoopType.LOOP);
    RawAnimation IDLE = RawAnimation.begin().then("move.idle", Animation.LoopType.LOOP);
    RawAnimation SIT = RawAnimation.begin().then("sleep.enter", Animation.LoopType.PLAY_ONCE);
    RawAnimation SITTING = RawAnimation.begin().then("sleep.idle", Animation.LoopType.LOOP);
    RawAnimation STAND_UP = RawAnimation.begin().then("sleep.wake_up", Animation.LoopType.PLAY_ONCE);

    AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    AnimationController<ShellSmasherEntity> genericController = new AnimationController<>(this, "generic", 3, (handler -> {
        if (this.isSitting()) {
            return handler.setAndContinue(SITTING);
        }

        if (handler.isMoving()) {
            handler.setControllerSpeed(1.3f);
            return handler.setAndContinue(WALK);
        }
        handler.setControllerSpeed(1f);
        return handler.setAndContinue(IDLE);
    })).setOverrideEasingType(EasingType.EASE_OUT_QUAD).triggerableAnim("sit", SIT).triggerableAnim("stand_up", STAND_UP);


    public ShellSmasherEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.ignoreCameraFrustum = true;
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
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9f)
                .add(EntityAttributes.GENERIC_JUMP_STRENGTH, 1f);
    }

    public void toggleSitting() {
        if (this.isSitting()) {
            stopSitting();
        } else {
            startSitting();
        }
    }

    public void startSitting() {
        if (this.isSitting()) {
            return;
        }

        this.playSound(SoundEvents.ENTITY_CAMEL_SIT);
        setInSittingPose(true);
        setSitting(true);

        this.setPose(EntityPose.SITTING);
        genericController.tryTriggerAnimation("sit");
        this.emitGameEvent(GameEvent.ENTITY_ACTION);
    }

    public void stopSitting() {
        if (!this.isSitting()) {
            return;
        }

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

        // sitting
        if (isTamed() && hand == Hand.MAIN_HAND && player.isSneaking() && !isBreedingItem(itemstack)) {
            toggleSitting();
            return ActionResult.SUCCESS;
        }

        // start riding
        if (!this.isBreedingItem(player.getStackInHand(hand)) && this.isSaddled() && !this.hasPassengers() && !player.shouldCancelInteraction()) {
            if (!this.getWorld().isClient) {
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

    private boolean isMovementLocked() {
        return standUpMovementLockTicks > 0 || this.isSitting();
    }

    @Override
    public void travel(Vec3d movementInput) {
        // Prevent movement while sitting or during stand-up lock
        if (this.isMovementLocked()) {
            super.travel(Vec3d.ZERO);
            return;
        }
        super.travel(movementInput);
    }

    // Riding and controlling
    @Override
    protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return super.getPassengerAttachmentPos(passenger, dimensions, scaleFactor).add(0, 0.05, 0);
    }

    // Dismounting
    @Nullable
    private Vec3d locateSafeDismountingPos(Vec3d offset, LivingEntity passenger) {
        double d = this.getX() + offset.x;
        double e = this.getBoundingBox().minY;
        double f = this.getZ() + offset.z;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        block0:
        for (EntityPose entityPose : passenger.getPoses()) {
            mutable.set(d, e, f);
            double g = this.getBoundingBox().maxY + 0.75;
            do {
                double h = this.getWorld().getDismountHeight(mutable);
                if ((double) mutable.getY() + h > g) continue block0;
                if (Dismounting.canDismountInBlock(h)) {
                    Box box = passenger.getBoundingBox(entityPose);
                    Vec3d vec3d = new Vec3d(d, (double) mutable.getY() + h, f);
                    if (Dismounting.canPlaceEntityAt(this.getWorld(), passenger, box.offset(vec3d))) {
                        passenger.setPose(entityPose);
                        return vec3d;
                    }
                }
                mutable.move(Direction.UP);
            } while ((double) mutable.getY() < g);
        }
        return null;
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        Vec3d vec3d = AbstractHorseEntity.getPassengerDismountOffset(this.getWidth(), passenger.getWidth(), this.getYaw() + (passenger.getMainArm() == Arm.RIGHT ? 90.0f : -90.0f));
        Vec3d vec3d2 = this.locateSafeDismountingPos(vec3d, passenger);
        if (vec3d2 != null) {
            return vec3d2;
        }
        Vec3d vec3d3 = AbstractHorseEntity.getPassengerDismountOffset(this.getWidth(), passenger.getWidth(), this.getYaw() + (passenger.getMainArm() == Arm.LEFT ? 90.0f : -90.0f));
        Vec3d vec3d4 = this.locateSafeDismountingPos(vec3d3, passenger);
        if (vec3d4 != null) {
            return vec3d4;
        }
        return this.getPos();
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
