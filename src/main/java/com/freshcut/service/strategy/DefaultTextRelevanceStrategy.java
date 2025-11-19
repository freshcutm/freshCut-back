package com.freshcut.service.strategy;

import java.util.Optional;

/**
 * Implementación por defecto del análisis de relevancia textual.
 */
public class DefaultTextRelevanceStrategy implements TextRelevanceStrategy {
    @Override
    public boolean isRelevantText(String text) {
        String t = Optional.ofNullable(text).orElse("").toLowerCase().trim();
        if (t.isEmpty()) return false;
        String[] domain = {
            // núcleo
            "corte","barba","estilo","estetica","estética","facciones","cara","rostro",
            "cabello","pelo","textura","tipo de cabello","barberia","barbería","degradado","fade",
            // rasgos faciales
            "frente","pomulos","pómulos","menton","mentón","mandibula","mandíbula","perfil","patillas","bigote",
            // formas de cara
            "oval","redond","triangular","diamante","cuadrad","alargad","estrech",
            // estilos comunes
            "pompadour","quiff","mullet","crop","crew","side part","linea","raya","buzz","undercut",
            // verbos contextuales
            "me queda","me favorece","recomend","suger","cambiar corte","barbero"
        };
        String[] off = {"clima","chiste","comida","politica","política","videojuego","programacion","programación","tarea","deberes","auto","mustang","coche","finanzas","medicina","juego","deporte"};
        int score = 0;
        for (String k : domain) if (t.contains(k)) score++;
        for (String k : off) if (t.contains(k)) score -= 1;
        return score >= 2;
    }
}