package net.sn0wix_.projectdragons.entity.shellsmasher;

import net.minecraft.util.Identifier;
import net.sn0wix_.projectdragons.ProjectDragons;
import net.sn0wix_.projectdragons.entity.custom.ShellSmasherEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;


// TODO fix: quick custom head anim flashes upon standing up and sitting down
public class ShellSmasherModel extends GeoModel<ShellSmasherEntity> {

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

        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        float pitch = entityData.headPitch() * ((float) Math.PI / 180F);
        float yaw = entityData.netHeadYaw() * ((float) Math.PI / 180F);

        pitch = (float) (pitch * Math.cos(pitch));
        yaw = (float) (yaw * Math.cos(yaw) * 2);

        neck.setRotX(neck.getRotX() + pitch / 2);
        neck.setRotY(neck.getRotY() + yaw / 2);

        head.setRotX(head.getRotX() + pitch / 2);
        head.setRotY(head.getRotY() + yaw / 2);
    }
}
