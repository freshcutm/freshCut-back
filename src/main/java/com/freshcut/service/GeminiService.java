package com.freshcut.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import com.freshcut.dto.ChatDtos.ChatRequest;
import com.freshcut.dto.ChatDtos.ChatResponse;
import com.freshcut.dto.ChatDtos.Message;

@Service
public class GeminiService {
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private final String apiKey;
    private final RestTemplate restTemplate;
    private static final String MODEL = "gemini-1.5-flash-latest";
    private static final String IMAGE_MODEL = "gemini-2.5-flash-image";
    private static final String STANDARD_REPLY = "Puedo ayudarte solo con recomendaciones de cortes, estilos, barba o facciones. ¿Quieres describir tu rostro o subir una foto?";

    public GeminiService(@Value("${gemini.api-key:}") String apiKeyProp) {
        String envKey = System.getenv("GEMINI_API_KEY");
        String key = (envKey != null && !envKey.isBlank()) ? envKey : apiKeyProp;
        this.apiKey = key != null ? key : "";
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    public ChatResponse chat(ChatRequest req) {
        StringBuilder all = new StringBuilder();
        if (req.getFaceDescription() != null) all.append(req.getFaceDescription()).append(" ");
        if (req.getMessages() != null) {
            for (Message m : req.getMessages()) {
                if (m.getContent() != null) all.append(m.getContent()).append(" ");
            }
        }
        if (!isRelevantText(all.toString())) {
            return new ChatResponse(STANDARD_REPLY);
        }
        if (apiKey == null || apiKey.isBlank()) {
            return new ChatResponse(STANDARD_REPLY);
        }

        String url = String.format(
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
            MODEL, apiKey);

        List<Map<String, Object>> contents = new ArrayList<>();

        String systemPrompt = "Eres un asistente especializado exclusivamente en recomendaciones de cortes de cabello basadas en rostro, barba, estilo y facciones. "
                + "Si el usuario pregunta algo fuera de este contexto, debes responder con: '" + STANDARD_REPLY + "'. "
                + "Nunca respondas con recomendaciones si no hay información válida. "
                + "Responde en español, muy breve (máximo 4 líneas), con 1-2 opciones y mantenimiento.";

        contents.add(Map.of(
            "role", "user",
            "parts", List.of(Map.of("text", systemPrompt))
        ));

        if (req.getFaceDescription() != null && !req.getFaceDescription().isBlank()) {
            contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", "Descripción del rostro del cliente: " + req.getFaceDescription()))
            ));
        }

        for (Message m : req.getMessages()) {
            String role = "user";
            if ("assistant".equalsIgnoreCase(m.getRole())) role = "model";
            contents.add(Map.of(
                "role", role,
                "parts", List.of(Map.of("text", m.getContent() == null ? "" : m.getContent()))
            ));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);
        Map<String, Object> gen = new HashMap<>();
        // Mayor variación controlada para respuestas más diversas
        gen.put("temperature", 0.75);
        gen.put("topK", 40);
        gen.put("topP", 0.9);
        gen.put("maxOutputTokens", 256);
        body.put("generationConfig", gen);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.postForObject(url, entity, Map.class);
            if (res == null) return new ChatResponse("No se obtuvo respuesta de la IA.");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) res.get("candidates");
            if (candidates == null || candidates.isEmpty()) return new ChatResponse("No tengo una recomendación en este momento.");
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return new ChatResponse("No tengo una recomendación en este momento.");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return new ChatResponse("No tengo una recomendación en este momento.");
            Object text = parts.get(0).get("text");
            return new ChatResponse(text == null ? "" : text.toString());
        } catch (HttpClientErrorException e) {
            return new ChatResponse(STANDARD_REPLY);
        } catch (RestClientException e) {
            return new ChatResponse(STANDARD_REPLY);
        }
    }

    public byte[] editHair(byte[] imageBytes, String contentType, String faceDescription, String style, Integer strength) {
        // Fallback sin clave: devolver la imagen original (no edición)
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[AI] GEMINI_API_KEY no está configurada. Se devuelve la imagen original sin edición.");
            return imageBytes;
        }

        String url = String.format(
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
            IMAGE_MODEL, apiKey);

        // Prompt de edición: mantener rostro/identidad, fondo e iluminación; cambiar sólo el cabello
        String prompt = buildImageEditPrompt(faceDescription, style, strength);

        Map<String, Object> inline = new HashMap<>();
        inline.put("mime_type", (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg");
        inline.put("data", Base64.getEncoder().encodeToString(imageBytes));

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("inline_data", inline));
        parts.add(Map.of("text", prompt));

        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", parts);

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));
        Map<String, Object> gen = new HashMap<>();
        gen.put("temperature", 0.6);
        gen.put("topK", 64);
        gen.put("topP", 0.9);
        // Fuerza respuesta como imagen para evitar respuestas de texto
        gen.put("response_mime_type", "image/png");
        body.put("generationConfig", gen);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.postForObject(url, entity, Map.class);
            if (res == null) return imageBytes;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) res.get("candidates");
            if (candidates == null || candidates.isEmpty()) return imageBytes;
            @SuppressWarnings("unchecked")
            Map<String, Object> c0 = (Map<String, Object>) candidates.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> contentOut = (Map<String, Object>) c0.get("content");
            if (contentOut == null) return imageBytes;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> partsOut = (List<Map<String, Object>>) contentOut.get("parts");
            if (partsOut == null) return imageBytes;
            for (Map<String, Object> p : partsOut) {
                @SuppressWarnings("unchecked")
                Map<String, Object> inlineOut = (Map<String, Object>) p.get("inline_data");
                if (inlineOut != null) {
                    Object data = inlineOut.get("data");
                    if (data instanceof String s) {
                        return Base64.getDecoder().decode(s);
                    }
                }
            }
            // Si no encontró imagen, devolver original
            log.warn("[AI] Respuesta sin imagen editada. Se devuelve original.");
            return imageBytes;
        } catch (HttpClientErrorException e) {
            log.error("[AI] Error HTTP en edición de cabello: {}", e.getResponseBodyAsString());
            return imageBytes;
        } catch (RestClientException e) {
            log.error("[AI] Error de cliente al contactar modelo: {}", e.getMessage());
            return imageBytes;
        }
    }

    public ChatResponse recommendFromPhoto(byte[] imageBytes, String contentType, String faceDescription) {
        if (!(isRelevantText(faceDescription) || isLikelyFacePhoto(imageBytes, contentType))) {
            return new ChatResponse(STANDARD_REPLY);
        }
        // Fallback sin clave: usa respuesta basada en reglas con la descripción
        if (apiKey == null || apiKey.isBlank()) {
            return new ChatResponse(STANDARD_REPLY);
        }

        String url = String.format(
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
            IMAGE_MODEL, apiKey);

        String prompt = buildPhotoAnalysisPrompt(faceDescription);

        Map<String, Object> inline = new HashMap<>();
        inline.put("mime_type", (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg");
        inline.put("data", Base64.getEncoder().encodeToString(imageBytes));

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("inline_data", inline));
        parts.add(Map.of("text", prompt));

        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", parts);

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));
        Map<String, Object> gen = new HashMap<>();
        // Aumenta diversidad para que no repita siempre las mismas opciones
        gen.put("temperature", 0.8);
        gen.put("topK", 40);
        gen.put("topP", 0.9);
        gen.put("maxOutputTokens", 256);
        body.put("generationConfig", gen);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.postForObject(url, entity, Map.class);
            if (res == null) return new ChatResponse("No se obtuvo respuesta de la IA.");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) res.get("candidates");
            if (candidates == null || candidates.isEmpty()) return new ChatResponse("No tengo una recomendación en este momento.");
            @SuppressWarnings("unchecked")
            Map<String, Object> contentOut = (Map<String, Object>) candidates.get(0).get("content");
            if (contentOut == null) return new ChatResponse("No tengo una recomendación en este momento.");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> partsOut = (List<Map<String, Object>>) contentOut.get("parts");
            if (partsOut == null || partsOut.isEmpty()) return new ChatResponse("No tengo una recomendación en este momento.");
            for (Map<String, Object> p : partsOut) {
                Object text = p.get("text");
                if (text != null) {
                    return new ChatResponse(text.toString());
                }
            }
            return new ChatResponse("No se obtuvo texto de recomendaciones.");
        } catch (HttpClientErrorException e) {
            return new ChatResponse(STANDARD_REPLY);
        } catch (RestClientException e) {
            return new ChatResponse(STANDARD_REPLY);
        }
    }

    private String buildPhotoAnalysisPrompt(String faceDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un asistente especializado exclusivamente en recomendaciones de cortes de cabello basadas en rostro, barba, estilo y facciones. ");
        sb.append("Si el usuario pregunta algo fuera de este contexto, debes responder con: '").append(STANDARD_REPLY).append("'. ");
        sb.append("Nunca respondas con recomendaciones si no hay información válida o si no se detecta rostro. ");
        sb.append("Analiza la foto del cliente y sugiere 2 cortes que le favorezcan según forma de rostro, frente, densidad y textura del cabello, y estilo personal. ");
        sb.append("Responde en español, breve (4–6 líneas), con nombres de cortes claros y 1–2 consejos de mantenimiento y productos. ");
        sb.append("Si aplica, sugiere un servicio del catálogo (Fade, Corte clásico, Barba). ");
        sb.append("Evita cualquier información médica. ");
        sb.append("Incluye al final una línea empezando con 'Evita:' que enumere 1–2 cortes o procedimientos que NO recomiendas para este caso (por ejemplo, fades muy altos si redondean más el rostro, químicos agresivos si el cabello es fino). ");
        if (faceDescription != null && !faceDescription.isBlank()) {
            sb.append("Notas del usuario: ").append(faceDescription).append(". ");
        }
        return sb.toString();
    }

    public boolean isRelevantText(String text) {
        String t = Optional.ofNullable(text).orElse("").toLowerCase().trim();
        if (t.isEmpty()) return false;
        String[] domain = {
            "corte","barba","estilo","estetica","estética","facciones","cara","rostro",
            "cabello","pelo","textura","tipo de cabello","barberia","barbería","degradado","fade",
            "pompadour","quiff","mullet","crop","crew","side part","linea","raya"
        };
        String[] off = {"clima","chiste","comida","politica","política","videojuego","programacion","programación","tarea","deberes","auto","mustang","coche","finanzas","medicina"};
        int score = 0;
        for (String k : domain) if (t.contains(k)) score++;
        for (String k : off) if (t.contains(k)) score -= 2;
        if (score <= 0) return false;
        // Señales de intención
        String[] intents = {"recomienda","sugerencia","me queda","me favorece","barbero","quiero","busco","cambiar","corte"};
        boolean intent = false; for (String s : intents) { if (t.contains(s)) { intent = true; break; } }
        return score >= 1 && intent;
    }

    public boolean isLikelyFacePhoto(byte[] imageBytes, String contentType) {
        try {
            if (imageBytes == null || imageBytes.length < 10240) return false; // muy pequeña
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) return false;
            int w = img.getWidth();
            int h = img.getHeight();
            if (w < 128 || h < 128) return false;
            // Muestreo simple de piel en zona central
            int cx0 = Math.max(0, w/2 - w/6);
            int cy0 = Math.max(0, h/2 - h/6);
            int cx1 = Math.min(w-1, w/2 + w/6);
            int cy1 = Math.min(h-1, h/2 + h/6);
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
            return ratio >= 0.03; // umbral bajo: presencia mínima de piel en zona central
        } catch (Exception e) {
            return false;
        }
    }

    private ChatResponse ruleBasedReply(ChatRequest req) {
        String face = Optional.ofNullable(req.getFaceDescription()).orElse("").toLowerCase();
        String text = "";
        if (req.getMessages() != null && !req.getMessages().isEmpty()) {
            Message m0 = req.getMessages().get(0);
            text = Optional.ofNullable(m0.getContent()).orElse("").toLowerCase();
        }

        String hair = extractSection(text, "pelo:");
        String tastes = extractSection(text, "gustos:");

        Random rng = new Random();
        String option1;
        String option2;

        if (tastes.contains("mohicano")) {
            String[] opt = {
                "Mohicano suavizado con laterales muy cortos (Fade alto).",
                "Mohicano moderno con transición media y textura en la cresta.",
                "Faux hawk con Fade medio para un perfil menos radical."
            };
            option1 = opt[rng.nextInt(opt.length)];
            option2 = rng.nextBoolean() ? "Crop texturizado si prefieres algo menos radical." : "Pompadour corto con laterales limpios como alternativa.";
        } else if (tastes.contains("mullet")) {
            String[] opt = {
                "Mullet moderno con Fade bajo y nuca marcada.",
                "Mullet suave con capas y transición media en laterales.",
                "Mullet clásico con textura arriba y contornos definidos."
            };
            option1 = opt[rng.nextInt(opt.length)];
            option2 = rng.nextBoolean() ? "Corte clásico con volumen y textura para transición al mullet." : "Crop largo con línea lateral para ir hacia mullet gradualmente.";
        } else if (face.contains("oval")) {
            String[] opt = {
                "Fade medio + textura arriba; favorece rostros ovalados.",
                "Side part con transición baja y volumen moderado.",
                "Crop desordenado con leve elevación en el frontal."
            };
            option1 = opt[rng.nextInt(opt.length)];
            option2 = rng.nextBoolean() ? "Pompadour ligero si buscas más altura." : "Peinado hacia atrás con fijación suave para pulir el perfil.";
        } else if (face.contains("redond") || face.contains("cachet")) {
            String[] opt = {
                "Corte clásico con volumen arriba y laterales bajos para estilizar.",
                "Quiff moderado con Fade medio para definir ángulos.",
                "Crop texturizado alto, manteniendo laterales muy contenidos."
            };
            option1 = opt[rng.nextInt(opt.length)];
            option2 = rng.nextBoolean() ? "Side part con Fade medio para definir pómulos." : "Peinado hacia atrás con raya discreta para afinar el contorno.";
        } else {
            String[] opt = {
                "Crop texturizado con Fade bajo, versátil para la mayoría.",
                "Fade medio con peine hacia atrás y acabado mate.",
                "Crew cut moderno con transición limpia y definición frontal."
            };
            option1 = opt[rng.nextInt(opt.length)];
            option2 = rng.nextBoolean() ? "Clásico peinado hacia atrás con laterales limpios." : "Side part suave con volumen natural arriba.";
        }

        String maintenance;
        if (hair.contains("grueso") || hair.contains("abundante")) {
            maintenance = rng.nextBoolean() ? "Mantenimiento: cera mate o polvo de volumen, repaso cada 3–4 semanas." : "Mantenimiento: crema moldeadora y control de volumen; repaso cada 3–4 semanas.";
        } else if (hair.contains("ondulado")) {
            maintenance = rng.nextBoolean() ? "Mantenimiento: crema para ondas y difusor; repaso cada 4 semanas." : "Mantenimiento: leave-in ligero y definición con manos; repaso cada 4 semanas.";
        } else if (hair.contains("liso")) {
            maintenance = rng.nextBoolean() ? "Mantenimiento: pomada ligera para definición; repaso cada 4 semanas." : "Mantenimiento: cera flexible y volumen moderado; repaso cada 4 semanas.";
        } else {
            maintenance = rng.nextBoolean() ? "Mantenimiento: producto ligero según textura; repaso cada 4 semanas." : "Mantenimiento: acabado mate y retoques rápidos; repaso cada 4 semanas.";
        }

        String notRec;
        if (hair.contains("fino") || hair.contains("delicado")) {
            notRec = rng.nextBoolean() ? "químicos agresivos y calor excesivo; productos pesados que apelmacen." : "decoloraciones fuertes y ceras pesadas que restan volumen.";
        } else if (face.contains("redond") || face.contains("cachet")) {
            notRec = rng.nextBoolean() ? "volumen en laterales y fades muy altos; copetes que redondeen más." : "líneas en laterales demasiado altas; volumen lateral exagerado.";
        } else if (face.contains("oval")) {
            notRec = rng.nextBoolean() ? "rapados extremos o copetes exagerados; químicos fuertes si cabello fino." : "volúmenes desproporcionados que alarguen demasiado; permanentes agresivas si cabello sensible.";
        } else if (hair.contains("grueso") || hair.contains("abundante")) {
            notRec = rng.nextBoolean() ? "copetes rígidos muy altos; cortes sin textura que dificulten el peinado." : "acabados brillantes muy pesados; contornos sin textura que endurecen.";
        } else if (hair.contains("ondulado")) {
            notRec = rng.nextBoolean() ? "coronilla demasiado corta; productos muy rígidos que frizzan." : "planos excesivos en el frontal; geles duros que generan frizz.";
        } else {
            notRec = rng.nextBoolean() ? "cortes demasiado altos si frente amplia; procedimientos químicos agresivos si cabello sensible." : "transiciones muy marcadas si rostro estrecho; tratamientos agresivos innecesarios.";
        }

        String reply = String.join("\n",
            "Opciones:",
            "1) " + option1,
            "2) " + option2,
            maintenance,
            "Evita: " + notRec
        );
        return new ChatResponse(reply);
    }

    private String extractSection(String text, String marker) {
        int i = text.indexOf(marker);
        if (i < 0) return "";
        int start = i + marker.length();
        int endLine = text.indexOf('\n', start);
        if (endLine < 0) endLine = text.length();
        return text.substring(start, endLine).trim();
    }

    private String buildImageEditPrompt(String faceDescription, String style, Integer strength) {
        StringBuilder sb = new StringBuilder();
        sb.append("Conserva identidad, facciones, iluminación y fondo de la imagen original. ");
        sb.append("No alteres la cara ni los rasgos; modifica únicamente el cabello con resultado fotorealista y coherente con las sombras existentes. ");
        int s = strength != null ? Math.max(0, Math.min(100, strength)) : 70;
        sb.append("Intensidad del cambio aproximada: ").append(s).append("/100. ");

        boolean wantsBald = false;
        if (style != null && !style.isBlank()) {
            String st = style.toLowerCase();
            wantsBald = st.contains("calvo") || st.contains("afeitado total") || st.contains("shaved") || st.contains("buzz zero");
            if (wantsBald) {
                sb.append("Aplica cabeza calva/afeitada completamente (buzz zero): elimina el cabello de la parte superior y laterales, mantén forma del cráneo natural, textura realista de cuero cabelludo y sombreado suave. No modifiques cejas ni barba si existen. ");
            } else {
                sb.append("Aplica de forma clara el estilo '").append(style).append("' con transiciones y volumen acordes al tipo y densidad de pelo, evitando líneas falsas en la implantación. ");
            }
        } else {
            sb.append("Selecciona automáticamente un corte favorecedor (fade medio o crop texturizado) según el rostro. ");
        }

        if (s >= 80) {
            sb.append("El cambio debe ser marcado y evidente respecto a la foto original, manteniendo naturalidad. ");
        } else if (s >= 50) {
            sb.append("Realiza un cambio visible pero moderado, con contornos coherentes. ");
        } else {
            sb.append("Haz un ajuste sutil y realista sin alterar la identidad. ");
        }

        sb.append("Evita cambios en piel, ojos, cejas, barba, ropa y fondo. ");
        if (faceDescription != null && !faceDescription.isBlank()) {
            sb.append("Notas del usuario: ").append(faceDescription).append(". ");
        }
        sb.append("Salida: imagen fotográfica sin texto superpuesto ni marcas, aspecto profesional.");
        return sb.toString();
    }
}