package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(GameRenderer.class)
public abstract class GameRendererReloadShaders {
  @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;

  @Inject(method = "render", at = @At("RETURN"))
  private void watut$capture(CallbackInfo ci) {
    ScreenParticleRenderer.getInstance().renderPending(featureRenderDispatcher);
  }

  @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
  private void watut$captureTarget(CallbackInfoReturnable<RenderTarget> cir) {
    RenderTarget target = ScreenParticleRenderer.captureTarget;
    if (target != null) cir.setReturnValue(target);
  }
}
