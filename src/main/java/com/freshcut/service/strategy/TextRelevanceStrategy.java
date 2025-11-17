package com.freshcut.service.strategy;

/**
 * Strategy para evaluar si un texto es relevante al dominio de cortes/estilos/barba.
 */
public interface TextRelevanceStrategy {
    boolean isRelevantText(String text);
}