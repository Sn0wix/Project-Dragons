package net.sn0wix_.projectdragons.entity.shellsmasher;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.sn0wix_.projectdragons.entity.custom.ShellSmasherEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ShellSmasherRenderer extends GeoEntityRenderer<ShellSmasherEntity> {
    public ShellSmasherRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new ShellSmasherModel());
    }

    @Override
    public void renderCubesOfBone(MatrixStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, int colour) {
        if (bone.getName().equals("saddle") || bone.getName().contains("armor")) {
            return;
        }

        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
    }
}
