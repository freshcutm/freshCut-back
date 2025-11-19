package com.freshcut.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.freshcut.service.strategy.FaceDetectionStrategy;
import com.freshcut.service.strategy.DefaultFaceDetectionStrategy;
import com.freshcut.service.strategy.TextRelevanceStrategy;
import com.freshcut.service.strategy.DefaultTextRelevanceStrategy;

@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private final String groqKey;
    private final RestTemplate restTemplate;
    private static final String GROQ_MODEL = "llama-3.1-8b-instant";
    private static final String GROQ_VISION_MODEL = "llama-3.2-11b-vision-preview";
    private static final String STANDARD_REPLY = "Puedo ayudarte solo con recomendaciones de cortes, estilos, barba o facciones. ¿Quieres describir tu rostro o subir una foto?";
    // Estrategias (Patrón Strategy) para relevancia de texto y detección de rostro
    private final TextRelevanceStrategy textStrategy = new DefaultTextRelevanceStrategy();
    private final FaceDetectionStrategy faceStrategy = new DefaultFaceDetectionStrategy();

    public AiService(@Value("${groq.api-key:}") String groqKeyProp) {
        String groqEnv = System.getenv("GROQ_API_KEY");
        this.groqKey = (groqEnv != null && !groqEnv.isBlank()) ? groqEnv : (groqKeyProp == null ? "" : groqKeyProp);
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
        if (groqKey == null || groqKey.isBlank()) {
            return new ChatResponse(STANDARD_REPLY);
        }

        String url = "https://api.groq.com/openai/v1/chat/completions";

        List<Map<String, Object>> messages = new ArrayList<>();
        String systemPrompt = "Eres un asistente especializado exclusivamente en recomendaciones de cortes de cabello, barba y estilos basados en las facciones del rostro. "
                + "Si el usuario pregunta algo fuera de este contexto, responde exactamente: '" + STANDARD_REPLY + "'. "
                + "Jamás generes recomendaciones si no hay información válida. No inventes detalles de la foto. No hables de otros temas.";
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (req.getFaceDescription() != null && !req.getFaceDescription().isBlank()) {
            messages.add(Map.of("role", "user", "content", "Descripción del rostro del cliente: " + req.getFaceDescription()));
        }
        for (Message m : req.getMessages()) {
            String role = "user";
            if ("assistant".equalsIgnoreCase(m.getRole())) role = "assistant";
            messages.add(Map.of("role", role, "content", Optional.ofNullable(m.getContent()).orElse("")));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", GROQ_MODEL);
        body.put("messages", messages);
        body.put("temperature", 0.4);
        body.put("max_tokens", 256);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.postForObject(url, entity, Map.class);
            if (res == null) return new ChatResponse(STANDARD_REPLY);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) res.get("choices");
            if (choices == null || choices.isEmpty()) return new ChatResponse(STANDARD_REPLY);
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return new ChatResponse(STANDARD_REPLY);
            Object content = message.get("content");
            String reply = content == null ? "" : content.toString();
            return new ChatResponse(reply);
        } catch (HttpClientErrorException e) {
            return new ChatResponse(STANDARD_REPLY);
        } catch (RestClientException e) {
            return new ChatResponse(STANDARD_REPLY);
        }
    }

    public byte[] editHair(byte[] imageBytes, String contentType, String faceDescription, String style, Integer strength) {
        throw new IllegalArgumentException("Servicio de edición no disponible en el plan actual");
    }

    public ChatResponse recommendFromPhoto(byte[] imageBytes, String contentType, String faceDescription) {
        boolean faceOk = isLikelyFacePhoto(imageBytes, contentType);
        boolean textOk = isRelevantText(faceDescription);
        if (!(textOk || faceOk)) {
            return new ChatResponse(STANDARD_REPLY);
        }
        if (groqKey == null || groqKey.isBlank()) {
            return new ChatResponse(STANDARD_REPLY);
        }

        String url = "https://api.groq.com/openai/v1/chat/completions";
        List<Map<String, Object>> messages = new ArrayList<>();
        String systemPrompt = "Eres un asistente especializado exclusivamente en recomendaciones de cortes de cabello, barba y estilos basados en las facciones del rostro. "
                + "Si el usuario pregunta algo fuera de este contexto, responde exactamente: '" + STANDARD_REPLY + "'. "
                + "Jamás generes recomendaciones si no hay información válida. No inventes detalles de la foto. No hables de otros temas. Si no se detecta rostro, responde con el mensaje estándar.";
        messages.add(Map.of("role", "system", "content", systemPrompt));

        List<Map<String, Object>> userContent = new ArrayList<>();
        userContent.add(Map.of("type", "text", "text", "Genera recomendaciones breves en español (máx 4 líneas), con 1–2 opciones y mantenimiento."));
        String mime = (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg";
        String b64 = Base64.getEncoder().encodeToString(imageBytes);
        userContent.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", "data:" + mime + ";base64," + b64)
        ));
        if (faceDescription != null && !faceDescription.isBlank()) {
            userContent.add(Map.of("type", "text", "text", "Notas del usuario: " + faceDescription));
        }
        messages.add(Map.of("role", "user", "content", userContent));

        Map<String, Object> body = new HashMap<>();
        body.put("model", GROQ_VISION_MODEL);
        body.put("messages", messages);
        body.put("temperature", 0.4);
        body.put("max_tokens", 256);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.postForObject(url, entity, Map.class);
            if (res == null) return new ChatResponse(STANDARD_REPLY);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) res.get("choices");
            if (choices == null || choices.isEmpty()) return new ChatResponse(STANDARD_REPLY);
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return new ChatResponse(STANDARD_REPLY);
            Object content = message.get("content");
            String reply = content == null ? "" : content.toString();
            return new ChatResponse(reply);
        } catch (HttpClientErrorException e) {
            return new ChatResponse(STANDARD_REPLY);
        } catch (RestClientException e) {
            return new ChatResponse(STANDARD_REPLY);
        }
    }

    public boolean isRelevantText(String text) {
        return textStrategy.isRelevantText(text);
    }

    public boolean isLikelyFacePhoto(byte[] imageBytes, String contentType) {
        return faceStrategy.isLikelyFacePhoto(imageBytes, contentType);
    }

}