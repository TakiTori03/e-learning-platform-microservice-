package com.hust.aiservice.service;

import com.hust.aiservice.entity.ChatMessage;
import com.hust.aiservice.entity.ChatSession;
import com.hust.aiservice.repository.ChatMessageRepository;
import com.hust.aiservice.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final GeminiService geminiService;

    @Value("${rag.max-history-pairs:3}")
    private int maxHistoryPairs;

    /** Tạo phiên mới hoặc tải phiên cũ */
    @Transactional
    public ChatSession getOrCreateSession(String sessionId, String userId, String courseId, String firstMessage) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            return sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ChatSession với ID: " + sessionId));
        }

        // Tạo phiên mới, title = 50 ký tự đầu của câu hỏi
        String title = firstMessage;
        if (title != null && title.length() > 50) {
            title = title.substring(0, 50) + "...";
        }

        ChatSession newSession = ChatSession.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .courseId(courseId)
                .title(title)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return sessionRepository.save(newSession);
    }

    /** Viết lại câu hỏi thành dạng độc lập dựa trên lịch sử */
    public String rewriteQuery(String sessionId, String currentMessage) {
        List<ChatMessage> history = getRecentHistory(sessionId);
        if (history.isEmpty()) {
            return currentMessage;  // Câu hỏi đầu tiên → giữ nguyên
        }

        StringBuilder historyText = new StringBuilder();
        for (ChatMessage msg : history) {
            historyText.append(String.format("[%s]: %s\n", msg.getRole(), msg.getContent()));
        }

        String rewritePrompt = String.format(
            "Dựa trên lịch sử hội thoại sau, viết lại câu hỏi mới nhất thành " +
            "một câu hỏi ĐỘC LẬP, HOÀN CHỈNH (standalone question) có thể hiểu được mà KHÔNG CẦN đọc lịch sử.\n\n" +
            "Lịch sử:\n%s\nCâu hỏi mới nhất: %s\n\n" +
            "Chỉ trả về câu hỏi đã viết lại bằng tiếng Việt, không giải thích gì thêm.",
            historyText, currentMessage
        );

        try {
            String rewritten = geminiService.generateResponse("Bạn là trợ lý viết lại truy vấn (Query Rewriter).", rewritePrompt);
            log.info("📝 Câu hỏi gốc: '{}' -> Câu hỏi viết lại: '{}'", currentMessage, rewritten);
            return rewritten.trim();
        } catch (Exception e) {
            log.warn("⚠️ Không thể viết lại truy vấn, sử dụng câu hỏi gốc: {}", e.getMessage());
            return currentMessage;
        }
    }

    /** Lấy tối đa 6 tin nhắn gần nhất (3 cặp hỏi-đáp) */
    public List<ChatMessage> getRecentHistory(String sessionId) {
        List<ChatMessage> recent = messageRepository.findRecentMessages(
            sessionId, PageRequest.of(0, maxHistoryPairs * 2)
        );
        // Đảo ngược để đúng thứ tự thời gian
        return recent.stream().sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt())).collect(Collectors.toList());
    }

    /** Lưu tin nhắn mới vào DB */
    @Transactional
    public ChatMessage saveMessage(String sessionId, String role, String content, String citationsJson) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .citations(citationsJson)
                .createdAt(LocalDateTime.now())
                .build();
        
        ChatMessage saved = messageRepository.save(message);
        
        // Cập nhật thời gian update cho session
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
        });

        return saved;
    }
}
