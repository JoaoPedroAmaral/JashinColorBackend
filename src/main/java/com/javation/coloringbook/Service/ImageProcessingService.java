package com.javation.coloringbook.Service;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;

@Service
public class ImageProcessingService {

    private static final int MAX_WIDTH = 3000;
    private static final int MAX_HEIGHT = 3000;

    /**
     * Converte uma imagem para um estilo sketch aplicando escala de cinza,
     * blur e detecção de bordas com filtro Sobel.
     *
     * @param original imagem original
     * @return imagem em estilo sketch
     */
    public static BufferedImage convertToSketch(BufferedImage original) {
        BufferedImage resized = resizeImageIfNeeded(original, MAX_WIDTH, MAX_HEIGHT);
        int width = resized.getWidth();
        int height = resized.getHeight();

        // Passo 1: converter para escala de cinza
        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = new Color(resized.getRGB(x, y));
                int grayLevel = (int)(0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
                int grayRGB = new Color(grayLevel, grayLevel, grayLevel).getRGB();
                gray.setRGB(x, y, grayRGB);
            }
        }

        BufferedImage blurred;
        if(width > 2500 || height > 2500){
            blurred = gray; // mais fino
        }
        else if (width > 1800 || height > 1800) {
            blurred = applyBoxBlur(applyBoxBlur(applyBoxBlur(gray))); // menos detalhe
        } else {
            blurred = applyBoxBlur(gray);
        }

        BufferedImage edges = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        int[][] sobelX = {
                {-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}
        };

        int[][] sobelY = {
                {-1, -2, -1},
                { 0,  0,  0},
                { 1,  2,  1}
        };

        int threshold;
        if(width > 2500 || height > 2500){
            threshold = 35; // Mais detalhes
        }
        else if (width > 1800 || height > 1800) {
            threshold = 40;
        } else {
            threshold = 60; // Menos detalhes, traço mais fino
        }

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int pixelX = 0;
                int pixelY = 0;

                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int rgb = new Color(blurred.getRGB(x + j, y + i)).getRed();
                        pixelX += sobelX[i + 1][j + 1] * rgb;
                        pixelY += sobelY[i + 1][j + 1] * rgb;
                    }
                }

                int magnitude = (int) Math.min(255, Math.sqrt(pixelX * pixelX + pixelY * pixelY));

                int edgeColor = (magnitude > threshold) ? 0 : 255;

                int edgeRGB = new Color(edgeColor, edgeColor, edgeColor).getRGB();
                edges.setRGB(x, y, edgeRGB);
            }
        }

        return edges;
    }

    /**
     * Redimensiona a imagem se exceder as dimensões máximas.
     */
    private static BufferedImage resizeImageIfNeeded(BufferedImage original, int maxWidth, int maxHeight) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return original;
        }

        double widthRatio = (double) maxWidth / width;
        double heightRatio = (double) maxHeight / height;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int)(width * ratio);
        int newHeight = (int)(height * ratio);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }

    /**
     * Aplica um blur simples (Box Blur 3x3) para reduzir ruído.
     */
    private static BufferedImage applyBoxBlur(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage blurred = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        int[][] kernel = {
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 1}
        };
        int kernelSum = 9; // soma total da máscara

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int sum = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int rgb = new Color(img.getRGB(x + kx, y + ky)).getRed();
                        sum += kernel[ky + 1][kx + 1] * rgb;
                    }
                }

                int avg = sum / kernelSum;
                int gray = clamp(avg);
                int rgb = new Color(gray, gray, gray).getRGB();
                blurred.setRGB(x, y, rgb);
            }
        }

        return blurred;
    }

    /**
     * Garante que o valor fique entre 0 e 255.
     */
    private static int clamp(int value) {
        return Math.min(255, Math.max(0, value));
    }
}
