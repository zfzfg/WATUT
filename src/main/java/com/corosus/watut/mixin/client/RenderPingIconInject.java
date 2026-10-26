package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public abstract class RenderPingIconInject {
  @Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
  private void watut$idleIcon(
      GuiGraphicsExtractor graphics, int width, int x, int y, PlayerInfo player, CallbackInfo ci) {
    if (WatutMod.getPlayerStatusManagerClient()
        .renderPingIconHook((PlayerTabOverlay) (Object) this, graphics, width, x, y, player))
      ci.cancel();
  }
}
