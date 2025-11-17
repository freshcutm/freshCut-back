package com.freshcut.service.strategy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

/**
 * Implementación por defecto del detector simple de rostro basado en muestreo de piel.
 */
public class DefaultFaceDetectionStrategy implements FaceDetectionStrategy {
    @Override
    public boolean isLikelyFacePhoto(byte[] imageBytes, String contentType) {
        try {
            if (imageBytes == null || imageBytes.length < 8000) return false; // muy pequeña
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) return false;
            int w = img.getWidth();
            int h = img.getHeight();
            if (w < 128 || h < 128) return false;
            // Muestreo de piel en zona central ampliada
            int cx0 = Math.max(0, w/2 - w/4);
            int cy0 = Math.max(0, h/2 - h/4);
            int cx1 = Math.min(w-1, w/2 + w/4);
            int cy1 = Math.min(h-1, h/2 + h/4);
            int samples = 0;
            int skin = 0;
            for (int y = cy0; y <= cy1; y += Math.max(1,(cy1-cy0)/20)) {
                for (int x = cx0; x <= cx1; x += Math.max(1,(cx1-cx0)/20)) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    // Conversión aproximada a YCbCr
                    double Y = 0.299*r + 0.587*g + 0.114*b;
                    double Cb = -0.168736*r - 0.331264*g + 0.5*b + 128;
                    double Cr = 0.5*r - 0.418688*g - 0.081312*b + 128;
                    boolean isSkin = (Cb >= 77 && Cb <= 127) && (Cr >= 133 && Cr <= 173) && (Y > 40 && Y < 230);
                    samples++;
                    if (isSkin) skin++;
                }
            }
            double ratio = samples > 0 ? (double)skin / samples : 0.0;
            return ratio >= 0.004; // umbral permisivo para iluminación variable
        } catch (Exception e) {
            return false;
        }
    }
}