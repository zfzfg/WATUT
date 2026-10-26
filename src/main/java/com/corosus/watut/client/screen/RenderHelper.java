package com.corosus.watut.client.screen;

import com.corosus.watut.PlayerStatus;
import com.corosus.watut.WatutMod;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.mixin.client.NativeImageAccessor;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

public final class RenderHelper {
  public static boolean performingOwnRender;
  public static final Identifier cursor =
      Identifier.fromNamespaceAndPath(WatutMod.MODID, "textures/misc/mouse.png");
  public static final ByteBufferProcessor processor =
      new ByteBufferProcessor(RenderHelper::compress);

  private RenderHelper() {}

  public static boolean useDynamicGUISystem() {
    return !ConfigServerControlledSyncedToClient.dynamicGuiUseOldSimpleGUIVisual
        && !ConfigClient.dontSendDetailedGUIInfo;
  }

  public static void guiRender(GuiGraphicsExtractor graphics) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;
    long gameTime = mc.level.getGameTime();
    for (PlayerStatus status :
        WatutMod.getPlayerStatusManagerClient().lookupPlayerToStatus.values()) {
      ScreenData data = status.getScreenData();
      if (!data.getIsBufferReady().get()
          || !data.needsNewRenderFromPixelData()
          || data.getDecompressionBuffer() == null
          || data.getGameTicksSinceLastScreenReceiveAndRender()
                  + ConfigClient.tickReceiveAndRenderRateOfGUIUpdates
              >= gameTime) continue;
      if (data.getImage() == null
          || data.getImage().getPixels().getWidth() != data.getWidth()
          || data.getImage().getPixels().getHeight() != data.getHeight()) {
        data.closeImage();
        data.setImage(
            new DynamicTexture("WATUT player GUI", data.getWidth(), data.getHeight(), true));
      }
      long pixels = ((NativeImageAccessor) (Object) data.getImage().getPixels()).pixels();
      int bytes = Math.multiplyExact(Math.multiplyExact(data.getWidth(), data.getHeight()), 4);
      if (pixels != 0 && data.getDecompressionBuffer().remaining() == bytes) {
        MemoryUtil.memCopy(MemoryUtil.memAddress(data.getDecompressionBuffer()), pixels, bytes);
        data.getImage().upload();
      }
      data.markNeedsNewRenderFromPixelData(false);
      data.setGameTicksSinceLastScreenReceiveAndRender(gameTime);
      data.getIsBufferReady().set(false);
    }
  }

  public static synchronized void renderWithTooltipEnd(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    if (!useDynamicGUISystem()) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null || mc.gui.screen() == null) return;
    PlayerStatus local = WatutMod.getPlayerStatusManagerClient().getStatusLocal();
    if (processor.hasProcessedBuffers()) {
      try {
        ByteBuffer result = processor.getProcessedBuffer();
        if (result != null) {
          local.getScreenData().setTexturePixelData(result);
          try {
            WatutMod.getPlayerStatusManagerClient().sendScreenRenderData(local);
          } finally {
            ScreenParticleRenderer.getInstance().captureComplete();
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    if (local.getScreenData().isNeedsNewRenderToPixelData() && !processor.hasWork()) {
      local.getScreenData().setNeedsNewRenderToPixelData(false);
      ScreenParticleRenderer.getInstance().extract(mc.gui.screen(), mouseX, mouseY, partialTick);
    }
  }

  public static ByteBuffer cropAndBlur(
      ByteBuffer source, int width, int height, int outWidth, int outHeight, double scale) {
    return GuiImageCodec.cropAndBlur(
        source,
        width,
        height,
        outWidth,
        outHeight,
        scale,
        ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen,
        ConfigServerControlledSyncedToClient.dynamicGuiBlurLevel);
  }

  public static ByteBuffer compress(ByteBuffer input) {
    return GuiImageCodec.compress(input);
  }

  public static ByteBuffer decompress(ScreenData data, ByteBuffer compressed, int expectedSize)
      throws Exception {
    byte[] output = GuiImageCodec.decompress(compressed, expectedSize);
    ByteBuffer buffer = data.getDecompressionBuffer();
    if (buffer == null || buffer.capacity() < expectedSize) {
      if (buffer != null) MemoryUtil.memFree(buffer);
      buffer = MemoryUtil.memAlloc(expectedSize);
      data.setDecompressionBuffer(buffer);
    }
    buffer.clear();
    buffer.put(output).flip();
    return buffer;
  }

  public static boolean validatePixelByteBuffer(
      ByteBuffer buffer, int expectedSize, int alignment) {
    return buffer != null && buffer.remaining() == expectedSize;
  }
}
