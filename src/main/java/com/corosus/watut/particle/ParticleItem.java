package com.corosus.watut.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ParticleItem extends ParticleRotating {
  public static final HashSet<String> itemBlacklist = new HashSet<>();
  public final ItemStack itemStack;
  public float xFrom, yFrom, zFrom, xTo, yTo, zTo;

  public ParticleItem(
      ClientLevel level,
      float brightness,
      ItemStack stack,
      float xFrom,
      float yFrom,
      float zFrom,
      float xTo,
      float yTo,
      float zTo) {
    super(level, xFrom, yFrom, zFrom);
    itemStack = stack.copy();
    this.xFrom = xFrom;
    this.yFrom = yFrom;
    this.zFrom = zFrom;
    this.xTo = xTo;
    this.yTo = yTo;
    this.zTo = zTo;
    quadSize = 1;
    rotationYaw = level.getRandom().nextFloat() * 360;
  }

  @Override
  public void tick() {
    super.tick();
    xo = x;
    yo = y;
    zo = z;
    if (age++ >= 6) remove();
  }

  public ParticleGroupRenderState extractItem(Camera camera, float partialTick) {
    ItemStackRenderState model = new ItemStackRenderState();
    Minecraft.getInstance()
        .getItemModelResolver()
        .updateForTopItem(model, itemStack, ItemDisplayContext.GROUND, level, null, 0);
    float progress = Math.min(1, (age + partialTick) / 3F);
    Vec3 pos =
        new Vec3(
                Mth.lerp(progress, xFrom, xTo),
                Mth.lerp(progress, yFrom, yTo),
                Mth.lerp(progress, zFrom, zTo))
            .subtract(camera.position());
    int light = getLightCoords(partialTick);
    float size = quadSize;
    float yaw = rotationYaw;
    return (collector, cameraState) -> {
      PoseStack pose = new PoseStack();
      pose.translate(pos.x, pos.y, pos.z);
      pose.scale(size, size, size);
      pose.rotate(Axis.YP.rotationDegrees(yaw));
      model.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
    };
  }
}
