package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.ScreenParticleRenderer;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenRenderBackground {
  @Inject(
      method = {"extractBackground", "extractTransparentBackground"},
      at = @At("HEAD"),
      cancellable = true)
  private void watut$background(CallbackInfo ci) {
    if (ScreenParticleRenderer.isRenderingParticleGUI) ci.cancel();
  }
}
