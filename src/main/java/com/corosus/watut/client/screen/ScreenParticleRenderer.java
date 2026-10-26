package com.corosus.watut.client.screen;

import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Vector4f;

/** Captures a separately extracted GUI through Minecraft's backend-independent GPU API. */
public final class ScreenParticleRenderer {
  public static boolean isRenderingParticleGUI, isRenderingParticleGUI2;
  public static RenderTarget captureTarget;
  public static final int defaultWidthScaledDown = 256,
      defaultHeightScaledDown = 256,
      bytesPerPixel = 4;
  private static final ScreenParticleRenderer INSTANCE = new ScreenParticleRenderer();
  public int width, height, widthScaledDown = 256, heightScaledDown = 256;
  public boolean needsInit = true;
  private final GuiRenderState state = new GuiRenderState();
  private TextureTarget target;
  private GuiRenderer renderer;
  private static java.util.function.BiFunction<GuiRenderState, FeatureRenderDispatcher, GuiRenderer>
      rendererFactory;

  public static void configure(
      java.util.function.BiFunction<GuiRenderState, FeatureRenderDispatcher, GuiRenderer> factory) {
    rendererFactory = factory;
  }

  private boolean pending, reading;

  public static ScreenParticleRenderer getInstance() {
    return INSTANCE;
  }

  public void captureComplete() {
    reading = false;
  }

  public void checkSetup() {
    Minecraft mc = Minecraft.getInstance();
    width = mc.getWindow().getWidth();
    height = mc.getWindow().getHeight();
    widthScaledDown =
        ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen
            ? width
            : defaultWidthScaledDown;
    heightScaledDown =
        ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen
            ? height
            : defaultHeightScaledDown;
  }

  public void extract(Screen screen, int mouseX, int mouseY, float partialTick) {
    if (pending || reading) return;
    checkSetup();
    state.reset();
    GuiGraphicsExtractor graphics =
        new GuiGraphicsExtractor(Minecraft.getInstance(), state, mouseX, mouseY);
    RenderHelper.performingOwnRender = true;
    isRenderingParticleGUI =
        ConfigServerControlledSyncedToClient.dynamicGuiDisableBackgroundRendering;
    isRenderingParticleGUI2 = isRenderingParticleGUI;
    try {
      screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, partialTick);
      graphics.blit(
          RenderPipelines.GUI_TEXTURED, RenderHelper.cursor, mouseX, mouseY, 0, 0, 6, 10, 6, 10);
      graphics.extractDeferredElements(mouseX, mouseY, partialTick);
      pending = true;
    } finally {
      RenderHelper.performingOwnRender = false;
      isRenderingParticleGUI = false;
      isRenderingParticleGUI2 = false;
    }
  }

  public void renderPending(FeatureRenderDispatcher features) {
    if (!pending || reading) return;
    pending = false;
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) {
      state.reset();
      return;
    }
    if (target == null || target.width != width || target.height != height || needsInit) {
      if (target != null) target.destroyBuffers();
      target =
          new TextureTarget(
              "WATUT GUI capture", width, height, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
      needsInit = false;
    }
    if (renderer == null) renderer = rendererFactory.apply(state, features);
    RenderSystem.backupProjectionMatrix();
    try {
      captureTarget = target;
      RenderSystem.getDevice()
          .createCommandEncoder()
          .clearColorAndDepthTextures(
              target.getColorTexture(), new Vector4f(0), target.getDepthTexture(), 0);
      renderer.render();
      renderer.endFrame();
    } finally {
      captureTarget = null;
      RenderSystem.restoreProjectionMatrix();
    }
    int sourceWidth = width,
        sourceHeight = height,
        outWidth = widthScaledDown,
        outHeight = heightScaledDown;
    double scale = mc.getWindow().getGuiScale();
    GpuBuffer readback =
        RenderSystem.getDevice()
            .createBuffer(
                () -> "WATUT GUI readback",
                GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ,
                (long) width * height * 4);
    reading = true;
    try {
      RenderSystem.getDevice()
          .createCommandEncoder()
          .copyTextureToBuffer(
              target.getColorTexture(),
              readback,
              0,
              () -> {
                try (var mapped = readback.map(true, false)) {
                  ByteBuffer pixels =
                      RenderHelper.cropAndBlur(
                          mapped.data(), sourceWidth, sourceHeight, outWidth, outHeight, scale);
                  var local =
                      com.corosus.watut.WatutMod.getPlayerStatusManagerClient()
                          .getStatusLocal()
                          .getScreenData();
                  local.setWidth(outWidth);
                  local.setHeight(outHeight);
                  RenderHelper.processor.submitForProcessing(pixels);
                } catch (RuntimeException e) {
                  reading = false;
                  throw e;
                } finally {
                  readback.close();
                }
              },
              0);
    } catch (RuntimeException e) {
      readback.close();
      reading = false;
      throw e;
    }
  }
}
