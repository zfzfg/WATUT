package com.corosus.watut.mixin.client;

import com.corosus.watut.PlayerStatusManagerClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
  @Inject(method = "tick", at = @At("TAIL"))
  private void watut$tick(CallbackInfo ci) {
    PlayerStatusManagerClient.getParticleEngine().tick();
  }

  @Inject(method = "extract", at = @At("TAIL"))
  private void watut$extract(
      ParticlesRenderState state,
      Frustum frustum,
      Camera camera,
      float partialTick,
      CallbackInfo ci) {
    PlayerStatusManagerClient.getParticleEngine().extract(state, camera, partialTick);
  }

  @Inject(method = "setLevel", at = @At("HEAD"))
  private void watut$level(ClientLevel level, CallbackInfo ci) {
    PlayerStatusManagerClient.getParticleEngine().setLevel(level);
  }
}
