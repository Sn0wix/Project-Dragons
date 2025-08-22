package net.sn0wix_.projectdragons.mixin.client;

import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.sn0wix_.projectdragons.entity.custom.ShellSmasherEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerJumpingMixin {
    @Shadow
    public Input input;

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick(ZF)V", shift = At.Shift.AFTER))
    private void inject(CallbackInfo ci) {
        if (((ClientPlayerEntity) (Object) this).getVehicle() instanceof ShellSmasherEntity shellSmasher) {
            shellSmasher.riderIsJumping = this.input.jumping;
        }
    }
}