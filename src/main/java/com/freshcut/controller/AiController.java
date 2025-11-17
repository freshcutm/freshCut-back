package com.freshcut.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.freshcut.dto.ChatDtos.ChatRequest;
import com.freshcut.dto.ChatDtos.ChatResponse;
import com.freshcut.dto.ChatDtos.Message;
import com.freshcut.db.model.ChatLog;
import com.freshcut.db.repository.ChatLogRepository;
import com.freshcut.security.JwtService;
import com.freshcut.service.GeminiService;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {
    private final GeminiService geminiService;
    private final ChatLogRepository chatLogRepository;
    private final JwtService jwtService;

    public AiController(GeminiService geminiService, ChatLogRepository chatLogRepository, JwtService jwtService) {
        this.geminiService = geminiService;
        this.chatLogRepository = chatLogRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest req,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        String email = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(authorization.substring(7));
                email = claims.getSubject();
            } catch (Exception ignored) {}
        }

        StringBuilder all = new StringBuilder();
        if (req.getFaceDescription() != null) all.append(req.getFaceDescription()).append(" ");
        if (req.getMessages() != null) {
            for (Message m : req.getMessages()) {
                if (m.getContent() != null) all.append(m.getContent()).append(" ");
            }
        }
        boolean relevant = geminiService.isRelevantText(all.toString());
        ChatResponse res;
        if (!relevant) {
            res = new ChatResponse("Puedo ayudarte solo con recomendaciones de cortes, estilos, barba o facciones. ¿Quieres describir tu rostro o subir una foto?");
        } else {
            res = geminiService.chat(req);
        }
        ChatLog log = new ChatLog();
        log.setEmail(email);
        log.setFaceDescription(req.getFaceDescription());
        List<ChatLog.Msg> msgs = new ArrayList<>();
        if (req.getMessages() != null) {
            for (Message m : req.getMessages()) {
                ChatLog.Msg mm = new ChatLog.Msg();
                mm.setRole(m.getRole());
                mm.setContent(m.getContent());
                msgs.add(mm);
            }
        }
        log.setMessages(msgs);
        log.setReply(res.getReply());
        if (!relevant) {
            log.setRejectReason("irrelevant_text");
        }
        chatLogRepository.save(log);

        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "/edit-hair", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> editHair(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "faceDescription", required = false) String faceDescription,
            @RequestParam(name = "style", required = false) String style,
            @RequestParam(name = "strength", required = false) Integer strength) {
        try {
            byte[] original = file.getBytes();
            byte[] out = geminiService.editHair(original, file.getContentType(), faceDescription, style, strength);
            boolean edited = !Arrays.equals(original, out);
            // Gemini suele devolver PNG para ediciones; usamos image/png por defecto
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header("X-AI-Edited", edited ? "true" : "false")
                    .body(out);
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

@PostMapping(value = "/recommend-from-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatResponse> recommendFromPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "faceDescription", required = false) String faceDescription,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        try {
            byte[] image = file.getBytes();
            boolean textOk = geminiService.isRelevantText(faceDescription);
            boolean faceOk = geminiService.isLikelyFacePhoto(image, file.getContentType());
            boolean relevant = faceOk || textOk;
            // Siempre devolver 200 OK. Si es irrelevante, GeminiService responde con STANDARD_REPLY.
            ChatResponse res = geminiService.recommendFromPhoto(image, file.getContentType(), faceDescription);

            String email = null;
            if (authorization != null && authorization.startsWith("Bearer ")) {
                try {
                    Claims claims = jwtService.parse(authorization.substring(7));
                    email = claims.getSubject();
                } catch (Exception ignored) {}
            }

            ChatLog log = new ChatLog();
            log.setEmail(email);
            log.setFaceDescription(faceDescription);
            log.setMessages(new ArrayList<>());
            log.setReply(res.getReply());
            if (!relevant) {
                log.setRejectReason(faceOk ? "irrelevant_text" : "no_face_detected");
            }
            chatLogRepository.save(log);

            // Devuelve siempre 200 OK para evitar errores de consola en el navegador.
            // Si es irrelevante, el frontend detectará STANDARD_REPLY y mostrará un mensaje en pantalla.
            return ResponseEntity.ok()
                    .header("X-Reject-Reason", relevant ? "" : (faceOk ? "irrelevant_text" : "no_face_detected"))
                    .body(res);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ChatResponse("Puedo ayudarte solo con recomendaciones de cortes, estilos, barba o facciones. ¿Quieres describir tu rostro o subir una foto?"));
        }
    }
}