package com.javation.coloringbook.Service;

import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

@Service
public class ImageProcessingService {

    private static final int THRESHOLD_VALUE = 40;
    private static final float[] GAUSSIAN_BLUR_KERNEL = {
            1/16f, 2/16f, 1/16f,
            2/16f, 4/16f, 2/16f,
            1/16f, 2/16f, 1/16f
    };

    public byte[] convertToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }

    public BufferedImage resizeForProcessing(BufferedImage original) {
        int maxDim = 800; // Aggressive cap for 512MB RAM
        int w = original.getWidth();
        int h = original.getHeight();

        if (w <= maxDim && h <= maxDim) return original;

        double scale = Math.min((double) maxDim / w, (double) maxDim / h);
        int newW = (int) (w * scale);
        int newH = (int) (h * scale);

        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newW, newH, null);
        g.dispose();

        return resized;
    }

    public BufferedImage preprocessForVectorization(BufferedImage original) {
        if (original == null) return null;

        int w = original.getWidth();
        int h = original.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = original.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int newVal = ((r + g + b) / 3 < 120) ? 0 : 255;
                result.setRGB(x, y, (newVal << 16) | (newVal << 8) | newVal);
            }
        }
        return result;
    }

    public BufferedImage convertToSketch(BufferedImage original) {
        if (original == null) throw new IllegalArgumentException("Original image cannot be null");

        int width = original.getWidth();
        int height = original.getHeight();

        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        java.awt.Graphics g = gray.getGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        BufferedImage blurred = applyGaussianBlur(gray);
        gray.flush(); // Free memory immediately

        int[] edges = applySobel(blurred);
        blurred.flush(); // Free memory immediately

        int[] cleanEdges = applyMedianFilter(edges, width, height);

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (cleanEdges[y * width + x] > THRESHOLD_VALUE) {
                    result.setRGB(x, y, 0xFF000000); // Black
                } else {
                    result.setRGB(x, y, 0xFFFFFFFF); // White
                }
            }
        }
        return result;
    }

    private BufferedImage applyGaussianBlur(BufferedImage src) {
        return new ConvolveOp(new Kernel(3, 3, GAUSSIAN_BLUR_KERNEL), ConvolveOp.EDGE_NO_OP, null).filter(src, null);
    }

    private int[] applySobel(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] pixels = new int[w * h];
        int[] out = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixels[y * w + x] = img.getRGB(x, y) & 0xFF;
            }
        }

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int p00 = pixels[(y - 1) * w + (x - 1)];
                int p02 = pixels[(y - 1) * w + (x + 1)];
                int p10 = pixels[y * w + (x - 1)];
                int p12 = pixels[y * w + (x + 1)];
                int p20 = pixels[(y + 1) * w + (x - 1)];
                int p22 = pixels[(y + 1) * w + (x + 1)];

                int gx = (-1 * p00) + p02 + (-2 * p10) + (2 * p12) + (-1 * p20) + p22;
                int gy = (-1 * p00) + (-2 * pixels[(y - 1) * w + x]) + (-1 * p02) + p20 + (2 * pixels[(y + 1) * w + x]) + p22;

                out[y * w + x] = (int) Math.min(255, Math.sqrt(gx * gx + gy * gy));
            }
        }
        return out;
    }

    private int[] applyMedianFilter(int[] edges, int width, int height) {
        int[] result = new int[edges.length];
        int[] neighborhood = new int[9];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int k = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        neighborhood[k++] = edges[(y + dy) * width + (x + dx)];
                    }
                }
                Arrays.sort(neighborhood);
                result[y * width + x] = neighborhood[4];
            }
        }
        return result;
    }
}