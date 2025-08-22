package net.sn0wix_.projectdragons.entity.shellsmasher;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.sn0wix_.projectdragons.ProjectDragons;
import net.sn0wix_.projectdragons.entity.custom.ShellSmasherEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.HashMap;
import java.util.Map;


// TODO fix: quick custom head anim flashes upon standing up and sitting down
public class ShellSmasherModel extends GeoModel<ShellSmasherEntity> {

    private final Map<Long, Float> smoothedTurnYaw = new HashMap<>();


    @Override
    public Identifier getModelResource(ShellSmasherEntity entity) {
        return Identifier.of(ProjectDragons.MOD_ID, "geo/shellsmasher.geo.json");
    }

    @Override
    public Identifier getTextureResource(ShellSmasherEntity entity) {
        return Identifier.of(ProjectDragons.MOD_ID, "textures/entity/shellsmasher/shellsmasher_" + entity.getVariant().asString() + ".png");
    }

    @Override
    public Identifier getAnimationResource(ShellSmasherEntity entity) {
        return Identifier.of(ProjectDragons.MOD_ID, "animations/shellsmasher.animation.json");
    }

    @Override
    public void setCustomAnimations(ShellSmasherEntity animatable, long instanceId, AnimationState<ShellSmasherEntity> animationState) {
        GeoBone neck = this.getAnimationProcessor().getBone("h_head");
        GeoBone head = this.getAnimationProcessor().getBone("h_head2");
        if (neck == null || head == null) return;

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

        float pitchRad = modelData.headPitch() * ((float) Math.PI / 180F);
        float yawRad = modelData.netHeadYaw() * ((float) Math.PI / 180F);

        if (!animatable.hasPassengers()) {
            applyRotation(pitchRad, yawRad, neck, head);

            decayToZero(instanceId, smoothedTurnYaw, 0.2f);
        } else {
            // Smooth only the riding side-glance based on body rotation
            final float maxSideGlanceDeg = 22.0f;   // limit how far the head glances due to turning

            float deltaYawDeg = MathHelper.wrapDegrees(animatable.getYaw() - animatable.prevYaw);
            float sideGlanceDeg = MathHelper.clamp(deltaYawDeg * 4.5f, -maxSideGlanceDeg, maxSideGlanceDeg);
            float targetTurnYaw = (float) -Math.toRadians(sideGlanceDeg) * 3; // scaling the movement
            float turnSmoothing = 0.02f; // smaller = slower/smoother


            if (animatable.hasControllingPassenger() && animatable.getControllingPassenger() instanceof PlayerEntity player) {
                targetTurnYaw = targetTurnYaw * (player.forwardSpeed >= 0 ? 1 : -1); // reverse head rotation when moving backwords
                //turnSmoothing = player.forwardSpeed != 0 ? 0.03f : 0.02f; // adaptive smoothing
            }

            float smoothTurn = smooth(instanceId, smoothedTurnYaw, targetTurnYaw, turnSmoothing);

            // Apply only yaw while ridden (no pitch), split across neck/head
            neck.setRotY(neck.getRotY() + smoothTurn * 0.45f);
            head.setRotY(head.getRotY() + smoothTurn * 0.55f);
        }
    }

    private void applyRotation(float pitch, float yaw, GeoBone neck, GeoBone head) {
        // Keep original feel for free look (no smoothing)
        pitch = (float) (pitch * Math.cos(pitch));
        yaw = (float) (yaw * Math.cos(yaw) * 2.0);

        neck.setRotX(neck.getRotX() + pitch * 0.5f);
        neck.setRotY(neck.getRotY() + yaw * 0.5f);

        head.setRotX(head.getRotX() + pitch * 0.5f);
        head.setRotY(head.getRotY() + yaw * 0.5f);
    }

    // Exponential smoothing toward target (for riding only)
    private float smooth(long id, Map<Long, Float> cache, float target, float alpha) {
        Float current = cache.get(id);
        float next = (current == null) ? target : MathHelper.lerp(alpha, current, target);
        cache.put(id, next);
        return next;
    }

    // Gently decay a smoothed value toward zero
    private void decayToZero(long id, Map<Long, Float> cache, float alpha) {
        Float current = cache.get(id);
        if (current == null) return;
        float next = MathHelper.lerp(alpha, current, 0.0f);
        if (Math.abs(next) < 1e-4f) {
            cache.remove(id);
        } else {
            cache.put(id, next);
        }
    }
}
