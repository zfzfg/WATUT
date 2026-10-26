package com.corosus.watut.client.screen;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** The GUI wire format is an exact-length zlib-compressed RGBA image. */
public final class GuiImageCodec {
  private GuiImageCodec() {}

  public static int rgbaBytes(int width, int height) {
    if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid GUI dimensions");
    long pixels = (long) width * height;
    if (pixels > 16 * 1024 * 1024) throw new IllegalArgumentException("GUI image is too large");
    return (int) (pixels * 4);
  }

  public static ByteBuffer cropAndBlur(
      ByteBuffer source,
      int width,
      int height,
      int outWidth,
      int outHeight,
      double scale,
      boolean entire,
      int blurRadius) {
    rgbaBytes(width, height);
    rgbaBytes(outWidth, outHeight);
    if (source.remaining() < rgbaBytes(width, height))
      throw new IllegalArgumentException("Incomplete source image");
    double cropWidth = entire ? width : outWidth * scale,
        cropHeight = entire ? height : outHeight * scale;
    double left = (width - cropWidth) / 2, top = (height - cropHeight) / 2;
    byte[] pixels = new byte[Math.multiplyExact(Math.multiplyExact(outWidth, outHeight), 4)];
    for (int y = 0; y < outHeight; y++)
      for (int x = 0; x < outWidth; x++) {
        int sx = (int) (left + (x + 0.5) * cropWidth / outWidth),
            sy = (int) (top + (y + 0.5) * cropHeight / outHeight);
        if (sx < 0 || sx >= width || sy < 0 || sy >= height) continue;
        int from = (sy * width + sx) * 4, to = (y * outWidth + x) * 4;
        for (int c = 0; c < 4; c++) pixels[to + c] = source.get(source.position() + from + c);
      }
    int radius = Math.min(32, Math.max(0, blurRadius));
    if (radius > 0) {
      byte[] pass = new byte[pixels.length];
      for (int axis = 0; axis < 2; axis++) {
        for (int y = 0; y < outHeight; y++)
          for (int x = 0; x < outWidth; x++)
            for (int c = 0; c < 4; c++) {
              int sum = 0;
              for (int d = -radius; d <= radius; d++) {
                int sx = Math.max(0, Math.min(outWidth - 1, x + (axis == 0 ? d : 0)));
                int sy = Math.max(0, Math.min(outHeight - 1, y + (axis == 1 ? d : 0)));
                sum += Byte.toUnsignedInt(pixels[(sy * outWidth + sx) * 4 + c]);
              }
              pass[(y * outWidth + x) * 4 + c] = (byte) (sum / (radius * 2 + 1));
            }
        byte[] swap = pixels;
        pixels = pass;
        pass = swap;
      }
    }
    return ByteBuffer.wrap(pixels);
  }

  public static ByteBuffer compress(ByteBuffer input) {
    byte[] bytes = new byte[input.remaining()];
    input.duplicate().get(bytes);
    Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
    try {
      deflater.setInput(bytes);
      deflater.finish();
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[] block = new byte[8192];
      while (!deflater.finished()) {
        int n = deflater.deflate(block);
        result.write(block, 0, n);
      }
      return ByteBuffer.wrap(result.toByteArray());
    } finally {
      deflater.end();
    }
  }

  public static byte[] decompress(ByteBuffer compressed, int expectedSize)
      throws java.util.zip.DataFormatException {
    if (expectedSize <= 0 || expectedSize > 64 * 1024 * 1024)
      throw new IllegalArgumentException("Invalid GUI image size");
    byte[] bytes = new byte[compressed.remaining()];
    compressed.duplicate().get(bytes);
    Inflater inflater = new Inflater();
    try {
      inflater.setInput(bytes);
      byte[] output = new byte[expectedSize];
      int count = 0;
      while (!inflater.finished() && count < output.length) {
        int n = inflater.inflate(output, count, output.length - count);
        if (n == 0) throw new IllegalArgumentException("Incomplete GUI image");
        count += n;
      }
      int overflow = inflater.finished() ? 0 : inflater.inflate(new byte[1]);
      if (count != expectedSize
          || overflow != 0
          || !inflater.finished()
          || inflater.getRemaining() != 0)
        throw new IllegalArgumentException("GUI image size mismatch");
      return output;
    } finally {
      inflater.end();
    }
  }
}
