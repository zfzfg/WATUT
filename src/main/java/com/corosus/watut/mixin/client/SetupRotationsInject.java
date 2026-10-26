package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import com.corosus.watut.client.emf.EmfPoseCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class SetupRotationsInject {
  @Inject(
      method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
      at = @At("RETURN"))
  private void watut$pose(AvatarRenderState state, CallbackInfo ci) {
    var level = Minecraft.getInstance().level;
    if (level != null && level.getEntity(state.id) instanceof Player player) {
      PlayerModel model = (PlayerModel) (Object) this;
      WatutMod.getPlayerStatusManagerClient()
          .setupRotationsHook(model, player, 0, 0, state.ageInTicks, 0, 0);
      EmfPoseCapture.capture(model, player);
    }
  }
}
