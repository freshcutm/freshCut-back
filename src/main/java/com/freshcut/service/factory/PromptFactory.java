package com.freshcut.service.factory;

public interface PromptFactory {
    String buildPhotoAnalysisPrompt(String standardReply, String faceDescription);
}