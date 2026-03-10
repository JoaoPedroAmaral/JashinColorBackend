package com.javation.coloringbook.Service;

import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

@Service
public class ImageProcessingService {

    // Threshold mais alto para garantir que apenas as linhas principais (mais finas) sejam mantidas
    private static final int THRESHOLD_VALUE = 65; 

    public byte[] convertToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }

    public BufferedImage resizeForProcessing(BufferedImage original) {
        int maxDim = 1000; // 1000px é o ideal para velocidade e RAM de 512MB
        int w = original.getWidth();
        int h = original.getHeight();
        
        if (w <= maxDim && h <= maxDim) return original;
        
        double scale = Math.min((double) maxDim / w, (double) maxDim / h);
        int newW = (int) (w * scale);
        int newH = (int) (h * scale);
        
        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resized.createGraphics();
        // Bilinear é mais rápido que Bicubic e suficiente para detecção de bordas
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

        int width = original.getWidth();
        int height = original.getHeight();

        // 1. Grayscale (Rápido)
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                pixels[y * width + x] = (r + g + b) / 3;
            }
        }

        // 2. Laplacian Filter (Gera linhas muito mais finas e precisas que Sobel)
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 1; y < height - 1; y++) {
            int offset = y * width;
            for (int x = 1; x < width - 1; x++) {
                // Kernel Laplaciano: 
                //  0 -1  0
                // -1  4 -1
                //  0 -1  0
                int p11 = pixels[offset + x];
                int p01 = pixels[offset - width + x];
                int p10 = pixels[offset + x - 1];
                int p12 = pixels[offset + x + 1];
                int p21 = pixels[offset + width + x];

                int laplacian = Math.abs(4 * p11 - (p01 + p10 + p12 + p21));
                
                // 3. Thresholding Direto (Sem filtro de mediana para máxima nitidez)
                if (laplacian > THRESHOLD_VALUE) {
                    result.setRGB(x, y, 0xFF000000); // Preto (Linha)
                } else {
                    result.setRGB(x, y, 0xFFFFFFFF); // Branco (Fundo)
                }
            }
        }
        
        // Limpa as bordas da imagem que o loop ignorou
        return result;
    }
}
