package net.sn0wix_.projectdragons.entity.shellsmasher;

import net.minecraft.util.Identifier;
import net.sn0wix_.projectdragons.ProjectDragons;
import net.sn0wix_.projectdragons.entity.custom.ShellSmasherEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

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
        return Identifier.of(ProjectDragons.MOD_ID, "animations/shellsmasher.animations.json");
    }

    @Override
    public void setCustomAnimations(ShellSmasherEntity animatable, long instanceId, AnimationState<ShellSmasherEntity> animationState) {
        GeoBone head = this.getAnimationProcessor().getBone("h_head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * ((float) Math.PI / 180F));
            head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180F));
        }
    }
}
