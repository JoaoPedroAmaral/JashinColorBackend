package com.javation.coloringbook.Service;

import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

@Service
public class ImageProcessingService {

    public byte[] convertToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }

    public BufferedImage resizeForProcessing(BufferedImage original) {
        int maxDim = 700; // Limite de segurança para 512MB RAM com muitas imagens
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
                int gray = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                int newVal = (gray < 130) ? 0 : 255;
                result.setRGB(x, y, (newVal << 16) | (newVal << 8) | newVal);
            }
        }
        return result;
    }

    public BufferedImage convertToSketch(BufferedImage original) {
        if (original == null) throw new IllegalArgumentException("Original image cannot be null");

        int w = original.getWidth();
        int h = original.getHeight();

        // Acesso direto aos pixels para evitar chamadas de função lentas em loop
        int[] inputPixels = original.getRGB(0, 0, w, h, null, 0, w);
        int[] gray = new int[w * h];
        long[] integral = new long[w * h];

        // 1. Grayscale e Imagem Integral (O(N))
        for (int y = 0; y < h; y++) {
            long rowSum = 0;
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                int rgb = inputPixels[offset + x];
                int luminance = ((rgb >> 16 & 0xFF) + (rgb >> 8 & 0xFF) + (rgb & 0xFF)) / 3;
                gray[offset + x] = luminance;
                rowSum += luminance;
                integral[offset + x] = (y == 0) ? rowSum : integral[offset - w + x] + rowSum;
            }
        }

        // 2. Limiarização Adaptativa para Traçado Fino
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int S = 8;     // Janela pequena = traço fino
        int T = 15;    // Sensibilidade 15%

        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                int x1 = Math.max(x - S, 1);
                int x2 = Math.min(x + S, w - 1);
                int y1 = Math.max(y - S, 1);
                int y2 = Math.min(y + S, h - 1);

                long count = (x2 - x1) * (y2 - y1);
                long sum = integral[y2 * w + x2] - integral[(y1 - 1) * w + x2] - integral[y2 * w + (x1 - 1)] + integral[(y1 - 1) * w + (x1 - 1)];

                if (gray[offset + x] * count < sum * (100 - T) / 100) {
                    result.setRGB(x, y, 0xFF000000); // Preto
                } else {
                    result.setRGB(x, y, 0xFFFFFFFF); // Branco
                }
            }
        }
        
        original.flush();
        return result;
    }
}
