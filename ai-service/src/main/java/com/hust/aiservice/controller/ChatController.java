package com.hust.aiservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.aiservice.dto.ChatRequest;
import com.hust.aiservice.dto.ChatResponse;
import com.hust.aiservice.entity.ChatMessage;
import com.hust.aiservice.entity.ChatSession;
import com.hust.aiservice.service.ChatSessionService;
import com.hust.aiservice.service.RagService;
import com.hust.aiservice.service.SemanticCacheService;
import com.hust.commonlibrary.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final RagService ragService;
    private final ChatSessionService chatSessionService;
    private final ObjectMapper objectMapper;
    private final SemanticCacheService semanticCacheService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody @Valid ChatRequest request) {
        log.info("📩 Nhận yêu cầu chat RAG cho Course [{}]: {}", request.getCourseId(), request.getMessage());
        
        // 1. Trích xuất userId từ Security Context
        String userId = SecurityUtils.getCurrentUserIdOrThrow();

        // 2. Lấy hoặc tạo phiên hội thoại mới
        ChatSession session = chatSessionService.getOrCreateSession(
                request.getSessionId(),
                userId,
                request.getCourseId(),
                request.getMessage()
        );

        String sessionId = session.getId();

        // 3. Viết lại câu hỏi độc lập (Standalone Query) dựa vào lịch sử
        String rewrittenMessage = chatSessionService.rewriteQuery(sessionId, request.getMessage());

        // 3.5. Kiểm tra Semantic Cache
        ChatResponse cachedResponse = semanticCacheService.getCachedResponse(request.getCourseId(), rewrittenMessage);
        if (cachedResponse != null) {
            log.info("🚀 [Cache Hit] Trả về câu trả lời đã cache cho session: {}", sessionId);
            // Lưu lịch sử hội thoại
            chatSessionService.saveMessage(sessionId, "user", request.getMessage(), null);
            String citationsJson = null;
            try {
                citationsJson = objectMapper.writeValueAsString(cachedResponse.getCitations());
            } catch (Exception e) {
                log.error("⚠️ Lỗi serialize citations từ cache: {}", e.getMessage());
            }
            chatSessionService.saveMessage(sessionId, "assistant", cachedResponse.getAnswer(), citationsJson);
            
            cachedResponse.setSessionId(sessionId);
            return ResponseEntity.ok(cachedResponse);
        }

        // 4. Lấy lịch sử hội thoại gần đây (để đính kèm vào User Prompt)
        List<ChatMessage> history = chatSessionService.getRecentHistory(sessionId);

        // 5. Tìm kiếm ngữ cảnh + Gọi Gemini sinh câu trả lời
        ChatResponse response = ragService.chatWithKnowledge(
                request.getCourseId(),
                request.getMessage(),
                rewrittenMessage,
                history
        );

        // 5.5. Lưu kết quả vào Semantic Cache
        semanticCacheService.saveToCache(request.getCourseId(), rewrittenMessage, response);

        // 6. Lưu tin nhắn User và Assistant vào CSDL
        chatSessionService.saveMessage(sessionId, "user", request.getMessage(), null);

        // Chuyển Citations sang JSON string để lưu vào db
        String citationsJson = null;
        try {
            citationsJson = objectMapper.writeValueAsString(response.getCitations());
        } catch (Exception e) {
            log.error("⚠️ Không thể serialize citations sang JSON: {}", e.getMessage());
        }
        
        chatSessionService.saveMessage(sessionId, "assistant", response.getAnswer(), citationsJson);

        // 7. Gửi lại sessionId trong response để Client tiếp tục hội thoại
        response.setSessionId(sessionId);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody @Valid ChatRequest request) {
        log.info("📩 Nhận yêu cầu stream chat RAG cho Course [{}]: {}", request.getCourseId(), request.getMessage());
        
        // 1. Trích xuất userId từ Security Context
        String userId = SecurityUtils.getCurrentUserIdOrThrow();

        // 2. Lấy hoặc tạo phiên hội thoại mới
        ChatSession session = chatSessionService.getOrCreateSession(
                request.getSessionId(),
                userId,
                request.getCourseId(),
                request.getMessage()
        );

        String sessionId = session.getId();

        // 3. Viết lại câu hỏi độc lập (Standalone Query) dựa vào lịch sử
        String rewrittenMessage = chatSessionService.rewriteQuery(sessionId, request.getMessage());

        // 3.5. Kiểm tra Semantic Cache
        ChatResponse cachedResponse = semanticCacheService.getCachedResponse(request.getCourseId(), rewrittenMessage);
        if (cachedResponse != null) {
            log.info("🚀 [Stream Cache Hit] Trả về dữ liệu từ cache cho session: {}", sessionId);
            // Lưu lịch sử
            chatSessionService.saveMessage(sessionId, "user", request.getMessage(), null);
            String citationsJson = null;
            try {
                citationsJson = objectMapper.writeValueAsString(cachedResponse.getCitations());
            } catch (Exception e) {
                log.error("⚠️ Lỗi serialize citations từ cache: {}", e.getMessage());
            }
            chatSessionService.saveMessage(sessionId, "assistant", cachedResponse.getAnswer(), citationsJson);

            // Gửi metadata event và message event
            String metadataJson = "{}";
            try {
                java.util.Map<String, Object> metaMap = new java.util.HashMap<>();
                metaMap.put("sessionId", sessionId);
                metaMap.put("citations", cachedResponse.getCitations());
                metadataJson = objectMapper.writeValueAsString(metaMap);
            } catch (Exception e) {
                log.error("⚠️ Lỗi serialize metadata: {}", e.getMessage());
            }

            ServerSentEvent<String> metadataEvent = ServerSentEvent.<String>builder()
                    .event("metadata")
                    .data(metadataJson)
                    .build();

            ServerSentEvent<String> messageEvent = ServerSentEvent.<String>builder()
                    .event("message")
                    .data(cachedResponse.getAnswer())
                    .build();

            return Flux.just(metadataEvent, messageEvent);
        }

        // 4. Lấy lịch sử hội thoại gần đây (để đính kèm vào User Prompt)
        List<ChatMessage> history = chatSessionService.getRecentHistory(sessionId);

        // 5. Trả về stream SSE từ RagService (RagService tự động lưu hội thoại & cache khi kết thúc stream)
        return ragService.chatStream(
                request.getCourseId(),
                request.getMessage(),
                rewrittenMessage,
                history,
                sessionId,
                chatSessionService
        );
    }
}

