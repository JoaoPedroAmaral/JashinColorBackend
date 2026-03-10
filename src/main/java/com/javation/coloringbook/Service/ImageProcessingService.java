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

    // Reduzi o threshold para pegar linhas mais finas e aumentei o contraste
    private static final int THRESHOLD_VALUE = 30; 
    
    // Kernel de Nitidez (Sharpen) para realçar bordas antes do processamento
    private static final float[] SHARPEN_KERNEL = {
        0f, -1f, 0f,
        -1f, 5f, -1f,
        0f, -1f, 0f
    };

    public byte[] convertToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }

    public BufferedImage resizeForProcessing(BufferedImage original) {
        int maxDim = 1000;
        int w = original.getWidth();
        int h = original.getHeight();
        
        if (w <= maxDim && h <= maxDim) return original;
        
        double scale = Math.min((double) maxDim / w, (double) maxDim / h);
        int newW = (int) (w * scale);
        int newH = (int) (h * scale);
        
        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, newW, newH, null);
        g.dispose();
        
        return resized;
    }

    public BufferedImage preprocessForVectorization(BufferedImage original) {
        if (original == null) return null;
        int w = original.getWidth();
        int h = original.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        
        // Algoritmo de limiarização mais "limpo"
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = original.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // Ponderação real de luminância (Rec. 601)
                double gray = 0.299 * r + 0.587 * g + 0.114 * b;
                int newVal = (gray < 140) ? 0 : 255;
                result.setRGB(x, y, (newVal << 16) | (newVal << 8) | newVal);
            }
        }
        return result;
    }

    public BufferedImage convertToSketch(BufferedImage original) {
        if (original == null) throw new IllegalArgumentException("Original image cannot be null");

        int width = original.getWidth();
        int height = original.getHeight();

        // 1. Converte para Cinza e Aplica Nitidez
        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        java.awt.Graphics2D g2 = gray.createGraphics();
        g2.drawImage(original, 0, 0, null);
        g2.dispose();

        // Aplica filtro de nitidez para destacar as linhas
        BufferedImage sharp = new ConvolveOp(new Kernel(3, 3, SHARPEN_KERNEL)).filter(gray, null);
        gray.flush();

        // 2. Extração de Bordas (Sobel Otimizado)
        int[] edges = applySobel(sharp);
        sharp.flush();

        // 3. Limpeza com filtro de mediana para remover ruídos pequenos
        int[] cleanEdges = applyMedianFilter(edges, width, height);

        // 4. Criação do resultado final em Binário (Economia extrema de memória)
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Invertemos a lógica: se é borda (edge > threshold), pintamos de PRETO.
                if (cleanEdges[y * width + x] > THRESHOLD_VALUE) {
                    result.setRGB(x, y, 0); // Preto (0 em TYPE_BYTE_BINARY)
                } else {
                    result.setRGB(x, y, 1); // Branco (1 em TYPE_BYTE_BINARY)
                }
            }
        }
        return result;
    }

    private int[] applySobel(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] pixels = new int[w * h];
        int[] out = new int[w * h];

        // Copia rápida de pixels
        img.getRaster().getSamples(0, 0, w, h, 0, pixels);

        for (int y = 1; y < h - 1; y++) {
            int offset = y * w;
            for (int x = 1; x < w - 1; x++) {
                int p00 = pixels[offset - w + x - 1];
                int p02 = pixels[offset - w + x + 1];
                int p10 = pixels[offset + x - 1];
                int p12 = pixels[offset + x + 1];
                int p20 = pixels[offset + w + x - 1];
                int p22 = pixels[offset + w + x + 1];

                // Sobel kernels
                int gx = (p02 + 2 * p12 + p22) - (p00 + 2 * p10 + p20);
                int gy = (p20 + 2 * pixels[offset + w + x] + p22) - (p00 + 2 * pixels[offset - w + x] + p02);

                int mag = (int) Math.sqrt(gx * gx + gy * gy);
                out[offset + x] = Math.min(255, mag);
            }
        }
        return out;
    }

    private int[] applyMedianFilter(int[] edges, int width, int height) {
        int[] result = new int[edges.length];
        int[] neighborhood = new int[9];

        for (int y = 1; y < height - 1; y++) {
            int offset = y * width;
            for (int x = 1; x < width - 1; x++) {
                neighborhood[0] = edges[offset - width + x - 1];
                neighborhood[1] = edges[offset - width + x];
                neighborhood[2] = edges[offset - width + x + 1];
                neighborhood[3] = edges[offset + x - 1];
                neighborhood[4] = edges[offset + x];
                neighborhood[5] = edges[offset + x + 1];
                neighborhood[6] = edges[offset + width + x - 1];
                neighborhood[7] = edges[offset + width + x];
                neighborhood[8] = edges[offset + width + x + 1];

                Arrays.sort(neighborhood);
                result[offset + x] = neighborhood[4];
            }
        }
        return result;
    }
}
