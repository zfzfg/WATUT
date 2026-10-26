package com.corosus.watut.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Rectangular, double-sided status cards, batched by Minecraft's native particle pipeline. */
public final class StatusQuadRenderState extends QuadParticleRenderState {
  private final float aspect;

  public StatusQuadRenderState(float aspect) {
    this.aspect = Math.max(0.01F, aspect);
  }

  @Override
  protected void renderRotatedQuad(
      VertexConsumer out,
      float x,
      float y,
      float z,
      float qx,
      float qy,
      float qz,
      float qw,
      float size,
      float u0,
      float u1,
      float v0,
      float v1,
      int color,
      int light) {
    Quaternionf rotation = new Quaternionf(qx, qy, qz, qw);
    Vector3f[] points = {
      new Vector3f(-size, -size / aspect, 0),
      new Vector3f(-size, size / aspect, 0),
      new Vector3f(size, size / aspect, 0),
      new Vector3f(size, -size / aspect, 0)
    };
    for (Vector3f point : points) point.rotate(rotation).add(x, y, z);
    float[] us = {u1, u1, u0, u0}, vs = {v1, v0, v0, v1};
    for (int i : new int[] {0, 1, 2, 3, 3, 2, 1, 0}) {
      Vector3f p = points[i];
      out.addVertex(p.x, p.y, p.z).setUv(us[i], vs[i]).setColor(color).setLight(light);
    }
  }
}
