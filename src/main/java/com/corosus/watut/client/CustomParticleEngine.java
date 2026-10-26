package com.corosus.watut.client;

import com.corosus.watut.particle.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.texture.TextureManager;

/** Uses Minecraft's native quad batching and keeps item-model submissions separate. */
public final class CustomParticleEngine {
  private final List<ParticleItem> items = new ArrayList<>();
  private final List<ParticleRotating> cards = new ArrayList<>();

  public CustomParticleEngine(ClientLevel level, TextureManager textures) {}

  public void add(Particle particle) {
    if (particle instanceof ParticleItem item) items.add(item);
    else if (particle instanceof ParticleRotating card) cards.add(card);
    else Minecraft.getInstance().particleEngine.add(particle);
  }

  public void tick() {
    items.removeIf(
        item -> {
          item.tick();
          return !item.isAlive();
        });
    cards.removeIf(
        card -> {
          card.tick();
          return !card.isAlive();
        });
  }

  public void setLevel(ClientLevel level) {
    items.clear();
    cards.clear();
  }

  public void extract(ParticlesRenderState state, Camera camera, float partialTick) {
    for (ParticleItem item : items) state.add(item.extractItem(camera, partialTick));
    StatusQuadRenderState icons = new StatusQuadRenderState(1);
    for (ParticleRotating card : cards) {
      if (card instanceof ParticleDynamic dynamic) {
        StatusQuadRenderState gui = new StatusQuadRenderState(dynamic.aspectRatio);
        dynamic.extract(gui, camera, partialTick);
        state.add(gui);
      } else card.extract(icons, camera, partialTick);
    }
    if (!icons.isEmpty()) state.add(icons);
  }
}
