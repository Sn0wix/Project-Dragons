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

    private final Map<Long, Float> smoothedTail0 = new HashMap<>();
    private final Map<Long, Float> smoothedTail1 = new HashMap<>();
    private final Map<Long, Float> smoothedTail2 = new HashMap<>();
    private final Map<Long, Float> smoothedTail3 = new HashMap<>();

    GeoBone neck = this.getAnimationProcessor().getBone("h_head");
    GeoBone head = this.getAnimationProcessor().getBone("h_head2");

    GeoBone tail_0 = this.getAnimationProcessor().getBone("tail");
    GeoBone tail_1 = this.getAnimationProcessor().getBone("section_1");
    GeoBone tail_2 = this.getAnimationProcessor().getBone("section_2");
    GeoBone tail_3 = this.getAnimationProcessor().getBone("section_3");


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
        if (neck == null || head == null || tail_0 == null || tail_1 == null || tail_2 == null || tail_3 == null) {
            neck = this.getAnimationProcessor().getBone("h_head");
            head = this.getAnimationProcessor().getBone("h_head2");
            tail_0 = this.getAnimationProcessor().getBone("tail");
            tail_1 = this.getAnimationProcessor().getBone("section1");
            tail_2 = this.getAnimationProcessor().getBone("section2");
            tail_3 = this.getAnimationProcessor().getBone("section3");
        }

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

        float pitchRad = modelData.headPitch() * ((float) Math.PI / 180F);
        float yawRad = modelData.netHeadYaw() * ((float) Math.PI / 180F);

        if (!animatable.hasPassengers()) {
            applyRotation(pitchRad, yawRad, neck, head);

            // Faster decay when not ridden to reduce lingering curl
            fastDecayToZero(instanceId, smoothedTurnYaw, 0.35f);
            fastDecayToZero(instanceId, smoothedTail0, 0.35f);
            fastDecayToZero(instanceId, smoothedTail1, 0.35f);
            fastDecayToZero(instanceId, smoothedTail2, 0.35f);
            fastDecayToZero(instanceId, smoothedTail3, 0.35f);
        } else {

            // Smooth only the riding side-glance based on body rotation
            final float maxSideGlanceDeg = 22.0f;   // limit how far the head glances due to turning

            float deltaYawDeg = MathHelper.wrapDegrees(animatable.getYaw() - animatable.prevYaw);
            float sideGlanceDeg = MathHelper.clamp(deltaYawDeg * 4.5f, -maxSideGlanceDeg, maxSideGlanceDeg);
            float targetTurnYaw = (float) -Math.toRadians(sideGlanceDeg) * 3; // scaling the movement
            float turnSmoothing = 0.02f; // smaller = slower/smoother

            if (animatable.hasControllingPassenger() && animatable.getControllingPassenger() instanceof PlayerEntity player) {
                targetTurnYaw = targetTurnYaw * (player.forwardSpeed >= 0 ? 1 : -1); // reverse head rotation when moving backwords
            }

            float smoothTurn = smooth(instanceId, smoothedTurnYaw, targetTurnYaw, turnSmoothing);

            // Apply only yaw while ridden (no pitch), split across neck/head
            neck.setRotY(neck.getRotY() + smoothTurn * 0.45f);
            head.setRotY(head.getRotY() + smoothTurn * 0.55f);

            // Tail smoothing: move opposite to head when ridden.
            // Use asymmetric smoothing: follow slowly, snap back faster.
            float tailTarget = -smoothTurn;

            // Base follow vs snap-to-zero alphas per segment
            final float f0 = 0.09f, s0 = 0.20f;
            final float f1 = 0.075f, s1 = 0.18f;
            final float f2 = 0.060f, s2 = 0.16f;
            final float f3 = 0.050f, s3 = 0.15f;

            float t0 = smoothAsymmetric(instanceId, smoothedTail0, tailTarget, f0, s0);
            float t1 = smoothAsymmetric(instanceId, smoothedTail1, t0, f1, s1);
            float t2 = smoothAsymmetric(instanceId, smoothedTail2, t1, f2, s2);
            float t3 = smoothAsymmetric(instanceId, smoothedTail3, t2, f3, s3);

            // Distribute rotations across the hierarchy (slightly reduced multipliers for snappier settle).
            tail_0.setRotY(tail_0.getRotY() + t0 * 0.45f);
            tail_1.setRotY(tail_1.getRotY() + t1 * 0.55f);
            tail_2.setRotY(tail_2.getRotY() + t2 * 0.65f);
            tail_3.setRotY(tail_3.getRotY() + t3 * 0.75f);
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

    // Exponential smoothing toward target (for riding only) - used for head
    private float smooth(long id, Map<Long, Float> cache, float target, float alpha) {
        Float current = cache.get(id);
        float next = (current == null) ? target : MathHelper.lerp(alpha, current, target);
        cache.put(id, next);
        return next;
    }

    // Asymmetric smoothing: follows target with alphaFollow, but returns toward zero with alphaSnap (faster)
    private float smoothAsymmetric(long id, Map<Long, Float> cache, float target, float alphaFollow, float alphaSnap) {
        Float current = cache.get(id);
        if (current == null) {
            cache.put(id, target);
            return target;
        }

        // If we're moving toward zero (|target| < |current|) or target reverses sign,
        // use a larger alpha to snap back quicker.
        boolean reversing = Math.signum(target) != Math.signum(current);
        boolean towardZero = Math.abs(target) < Math.abs(current);

        float alpha = (reversing || towardZero) ? alphaSnap : alphaFollow;
        float next = MathHelper.lerp(alpha, current, target);

        // Nudge tiny values to zero to avoid lingering
        if (Math.abs(next) < 1e-3f) next = 0.0f;

        cache.put(id, next);
        return next;
    }

    // Faster decay to zero when not ridden
    private void fastDecayToZero(long id, Map<Long, Float> cache, float alpha) {
        Float current = cache.get(id);
        if (current == null) return;
        float next = MathHelper.lerp(alpha, current, 0.0f);
        if (Math.abs(next) < 1e-3f) {
            cache.remove(id);
        } else {
            cache.put(id, next);
        }
    }
}
