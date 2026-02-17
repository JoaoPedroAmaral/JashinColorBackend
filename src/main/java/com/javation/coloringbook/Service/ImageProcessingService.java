package com.javation.coloringbook.Service;


import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Service responsible for converting images into clean,
 * black and white coloring book sketches.
 * Optimized for performance and low memory consumption.
 */
@Service
public class ImageProcessingService {

    private static final int THRESHOLD_VALUE = 80;
    private static final int CONTRAST_FACTOR = 2;

    /**
     * Main pipeline method to convert an original image into a sketch.
     *
     * @param original The uploaded BufferedImage.
     * @return A pure black and white BufferedImage (Coloring book style).
     */
    public BufferedImage convertToSketch(BufferedImage original) {
        if (original == null) {
            throw new IllegalArgumentException("Original image cannot be null");
        }

        int width = original.getWidth();
        int height = original.getHeight();
        int totalPixels = width * height;

        // 1. Extract raw pixels (Fastest way to read image data)
        int[] pixels = new int[totalPixels];
        original.getRGB(0, 0, width, height, pixels, 0, width);

        // 2. Execute the processing pipeline using primitive arrays for speed
        int[] grayPixels = convertToGrayscale(pixels);
        int[] edgePixels = applyEdgeDetection(grayPixels, width, height);
        increaseContrast(edgePixels);
        removeNoise(edgePixels, width, height);
        int[] finalPixels = thresholdToBlackAndWhite(edgePixels);

        // 3. Reconstruct the final pure B&W image
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        result.setRGB(0, 0, width, height, finalPixels, 0, width);

        return result;
    }

    /**
     * Converts RGB pixels to grayscale using fast bitwise operations.
     */
    private int[] convertToGrayscale(int[] pixels) {
        int[] gray = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = (p >> 16) & 0xff;
            int g = (p >> 8) & 0xff;
            int b = p & 0xff;

            // Fast luminosity formula using integer math instead of floats
            gray[i] = (r * 77 + g * 150 + b * 29) >> 8;
        }
        return gray;
    }

    /**
     * Applies a Sobel operator manually to detect edges.
     */
    private int[] applyEdgeDetection(int[] gray, int width, int height) {
        int[] edges = new int[gray.length];

        // Loop avoiding the borders (1 to width-1, 1 to height-1)
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int p00 = gray[(y - 1) * width + (x - 1)];
                int p01 = gray[(y - 1) * width + x];
                int p02 = gray[(y - 1) * width + (x + 1)];
                int p10 = gray[y * width + (x - 1)];
                // p11 is the center pixel, skipped in Sobel
                int p12 = gray[y * width + (x + 1)];
                int p20 = gray[(y + 1) * width + (x - 1)];
                int p21 = gray[(y + 1) * width + x];
                int p22 = gray[(y + 1) * width + (x + 1)];

                // Sobel X and Y gradients
                int gx = -p00 + p02 - 2 * p10 + 2 * p12 - p20 + p22;
                int gy = -p00 - 2 * p01 - p02 + p20 + 2 * p21 + p22;

                // Absolute sum is faster than Math.sqrt(gx*gx + gy*gy) and highly effective here
                int magnitude = Math.abs(gx) + Math.abs(gy);

                edges[y * width + x] = Math.min(255, magnitude);
            }
        }
        return edges;
    }

    /**
     * Amplifies the edge strengths to make lines pop.
     */
    private void increaseContrast(int[] edges) {
        for (int i = 0; i < edges.length; i++) {
            int val = edges[i] * CONTRAST_FACTOR;
            edges[i] = Math.min(255, val); // Cap at max grayscale value
        }
    }

    /**
     * Removes isolated pixels and weak artifacts to clean the drawing.
     * Operates in-place.
     */
    private void removeNoise(int[] edges, int width, int height) {
        int[] temp = new int[edges.length];
        System.arraycopy(edges, 0, temp, 0, edges.length);

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int idx = y * width + x;

                // If it's considered an edge, check its neighborhood
                if (temp[idx] > THRESHOLD_VALUE / 2) {
                    int neighbors = 0;
                    if (temp[idx - width - 1] > 0) neighbors++;
                    if (temp[idx - width] > 0) neighbors++;
                    if (temp[idx - width + 1] > 0) neighbors++;
                    if (temp[idx - 1] > 0) neighbors++;
                    if (temp[idx + 1] > 0) neighbors++;
                    if (temp[idx + width - 1] > 0) neighbors++;
                    if (temp[idx + width] > 0) neighbors++;
                    if (temp[idx + width + 1] > 0) neighbors++;

                    // If it has fewer than 2 connected edge pixels, it's a stray dot (noise)
                    if (neighbors < 2) {
                        edges[idx] = 0;
                    }
                }
            }
        }
    }

    /**
     * Converts the continuous edge map into pure Black (outlines) and White (background).
     */
    private int[] thresholdToBlackAndWhite(int[] edges) {
        int[] binaryPixels = new int[edges.length];

        int pureWhite = 0xFFFFFFFF;
        int pureBlack = 0xFF000000;

        // Default fill everything with white (handles edges of the image)
        Arrays.fill(binaryPixels, pureWhite);

        for (int i = 0; i < edges.length; i++) {
            // High edge magnitude = Line (Black), Low magnitude = Background (White)
            if (edges[i] > THRESHOLD_VALUE) {
                binaryPixels[i] = pureBlack;
            }
        }
        return binaryPixels;
    }
}