package com.corosus.watut.mixin.client;

import com.corosus.watut.client.emf.EmfPoseCapture;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs after WATUT's own setupAnim inject (default priority 1000) so the captured pose is final.
 */
@Mixin(value = PlayerModel.class, priority = 2500)
public class EmfPoseCaptureInject {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void watut$captureEmfPose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        EmfPoseCapture.capture((PlayerModel<?>) (Object) this, entity);
    }
}
