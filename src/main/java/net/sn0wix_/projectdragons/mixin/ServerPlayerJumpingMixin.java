package net.sn0wix_.projectdragons.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.sn0wix_.projectdragons.entity.custom.ShellSmasherEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerJumpingMixin {
    @Inject(method = "updateInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSneaking(Z)V"))
    private void inject(float sidewaysSpeed, float forwardSpeed, boolean jumping, boolean sneaking, CallbackInfo ci) {
        if (((ServerPlayerEntity) (Object) this).getVehicle() instanceof ShellSmasherEntity shellSmasher) {
            shellSmasher.riderIsJumping = jumping;
        }
    }
}