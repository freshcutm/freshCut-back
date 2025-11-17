package com.freshcut.service.strategy;

/**
 * Strategy para estimar si una imagen probablemente contiene un rostro.
 */
public interface FaceDetectionStrategy {
    boolean isLikelyFacePhoto(byte[] imageBytes, String contentType);
}