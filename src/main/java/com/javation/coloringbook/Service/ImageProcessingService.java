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

    // ... existing constants ...

    public byte[] convertToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private static final int THRESHOLD_VALUE = 40;
    private static final float[] GAUSSIAN_BLUR_KERNAL = {
        1/16f, 2/16f, 1/16f,
        2/16f, 4/16f, 2/16f,
        1/16f, 2/16f, 1/16f
    };

    public BufferedImage convertToSketch(BufferedImage original) {
        if (original == null) {
            throw new IllegalArgumentException("Original image cannot be null");
        }

        // 1. Convert to Grayscale
        BufferedImage gray = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        gray.getGraphics().drawImage(original, 0, 0, null);

        // 2. Apply Gaussian Blur to reduce initial noise
        BufferedImage blurred = applyGaussianBlur(gray);

        // 3. Sobel Edge Detection
        int width = blurred.getWidth();
        int height = blurred.getHeight();
        int[] edges = applySobel(blurred);

        // 4. Median Filter to remove "salt and pepper" noise from edges
        int[] cleanEdges = applyMedianFilter(edges, width, height);

        // 5. Thresholding to create clean black lines
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] finalPixels = new int[width * height];
        for (int i = 0; i < cleanEdges.length; i++) {
            if (cleanEdges[i] > THRESHOLD_VALUE) {
                finalPixels[i] = 0xFF000000; // Black line
            } else {
                finalPixels[i] = 0xFFFFFFFF; // White background
            }
        }

        result.setRGB(0, 0, width, height, finalPixels, 0, width);
        return result;
    }

    private BufferedImage applyGaussianBlur(BufferedImage src) {
        Kernel kernel = new Kernel(3, 3, GAUSSIAN_BLUR_KERNAL);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(src, null);
    }

    private int[] applySobel(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int[] pixels = new int[width * height];
        int[] out = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = img.getRGB(x, y) & 0xFF;
            }
        }

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int p00 = pixels[(y - 1) * width + (x - 1)];
                int p02 = pixels[(y - 1) * width + (x + 1)];
                int p10 = pixels[y * width + (x - 1)];
                int p12 = pixels[y * width + (x + 1)];
                int p20 = pixels[(y + 1) * width + (x - 1)];
                int p22 = pixels[(y + 1) * width + (x + 1)];

                int gx = (-1 * p00) + (1 * p02) + (-2 * p10) + (2 * p12) + (-1 * p20) + (1 * p22);
                int gy = (-1 * p00) + (-2 * pixels[(y - 1) * width + x]) + (-1 * p02) + (1 * p20) + (2 * pixels[(y + 1) * width + x]) + (1 * p22);

                double magnitude = Math.sqrt(gx * gx + gy * gy);
                out[y * width + x] = (int) Math.min(255, magnitude);
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
                result[y * width + x] = neighborhood[4]; // Median value
            }
        }
        return result;
    }
}
