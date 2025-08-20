package net.sn0wix_.projectdragons.entity.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.VariantHolder;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ShellSmasherEntity extends AnimalEntity implements GeoEntity, VariantHolder<ShellSmasherVariants> {
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(ShellSmasherEntity.class, TrackedDataHandlerRegistry.INTEGER);

    RawAnimation WALK = RawAnimation.begin().then("move.walk", Animation.LoopType.LOOP);
    RawAnimation IDLE = RawAnimation.begin().then("move.idle", Animation.LoopType.LOOP);

    AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ShellSmasherEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2f)
                .add(EntityAttributes.GENERIC_SAFE_FALL_DISTANCE, 6)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 25)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9f);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Override
    public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }



    // Variants
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(VARIANT, 0); // store plain enum ID
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getEntityVariant()); // write plain ID
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setEntityVariant(nbt.getInt("Variant")); // read plain ID
    }

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
        controllers.add(new AnimationController<>(this, 3, (handler -> {
            if (handler.isMoving()) {
                handler.setControllerSpeed(1.3f);
                return handler.setAndContinue(WALK);
            }

            handler.setControllerSpeed(1f);
            return handler.setAndContinue(IDLE);
        })));
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
