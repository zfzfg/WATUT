package com.corosus.watut.client.screen;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GuiImageCodecTest {
  @Test
  void roundTripsFullRgbaFrameWithoutChangingInputPosition() throws Exception {
    byte[] image = new byte[GuiImageCodec.rgbaBytes(256, 256)];
    new Random(42).nextBytes(image);
    ByteBuffer input = ByteBuffer.wrap(image);
    ByteBuffer compressed = GuiImageCodec.compress(input);
    assertEquals(0, input.position());
    assertArrayEquals(image, GuiImageCodec.decompress(compressed, image.length));
    assertEquals(0, compressed.position());
  }

  @Test
  void rejectsCompressedLengthUsedAsPixelLength() {
    ByteBuffer compressed = GuiImageCodec.compress(ByteBuffer.wrap(new byte[256 * 256 * 4]));
    assertThrows(
        IllegalArgumentException.class,
        () -> GuiImageCodec.decompress(compressed, compressed.remaining()));
  }

  @Test
  void rejectsTruncatedAndOversizedImages() {
    ByteBuffer compressed = GuiImageCodec.compress(ByteBuffer.wrap(new byte[64]));
    ByteBuffer truncated = compressed.duplicate();
    truncated.limit(truncated.limit() - 2);
    assertThrows(IllegalArgumentException.class, () -> GuiImageCodec.decompress(truncated, 64));
    assertThrows(IllegalArgumentException.class, () -> GuiImageCodec.decompress(compressed, 32));
  }

  @Test
  void cropsCenteredPixelsAndRespectsBufferPosition() {
    ByteBuffer image = ByteBuffer.allocate(4 + 4 * 4 * 4);
    image.position(4);
    for (int i = 0; i < 16; i++) for (int c = 0; c < 4; c++) image.put((byte) i);
    image.flip().position(4);
    ByteBuffer crop = GuiImageCodec.cropAndBlur(image, 4, 4, 2, 2, 1, false, 0);
    byte[] expected = new byte[16];
    int[] values = {5, 6, 9, 10};
    for (int i = 0; i < 4; i++) Arrays.fill(expected, i * 4, i * 4 + 4, (byte) values[i]);
    assertArrayEquals(expected, crop.array());
    assertEquals(4, image.position());
  }

  @Test
  void blurPreservesUniformColorAndAlpha() {
    byte[] image = new byte[8 * 8 * 4];
    Arrays.fill(image, (byte) 123);
    assertArrayEquals(
        image, GuiImageCodec.cropAndBlur(ByteBuffer.wrap(image), 8, 8, 8, 8, 1, true, 2).array());
  }

  @Test
  void dimensionsRejectOverflowAndEmptyImages() {
    assertEquals(256 * 256 * 4, GuiImageCodec.rgbaBytes(256, 256));
    assertThrows(IllegalArgumentException.class, () -> GuiImageCodec.rgbaBytes(0, 256));
    assertThrows(
        IllegalArgumentException.class,
        () -> GuiImageCodec.rgbaBytes(Integer.MAX_VALUE, Integer.MAX_VALUE));
  }
}
