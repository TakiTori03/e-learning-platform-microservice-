package com.hust.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.aiservice.dto.ChatResponse;
import com.hust.aiservice.dto.Citation;
import com.hust.aiservice.dto.SearchResultDto;
import com.hust.aiservice.entity.ChatMessage;
import com.hust.commonlibrary.entity.ContentType;
import com.hust.aiservice.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final GeminiService geminiService;
    private final DocumentChunkRepository documentChunkRepository;
    private final ObjectMapper objectMapper;
    private final SemanticCacheService semanticCacheService;
    private final CohereRerankService cohereRerankService;

    @Value("${rag.search-limit:20}")
    private int searchLimit;

    @Value("${rag.top-k-context:5}")
    private int topKContext;

    @Value("${rag.rrf-k:60.0}")
    private double rrfK;

    @Value("${rag.rerank-top-k:2}")
    private int rerankTopK;

    @Value("${rag.context-window-size:1}")
    private int contextWindowSize;

    @Value("${rag.fallback-answer:Xin lỗi, tôi chưa tìm thấy tài liệu học tập nào liên quan đến câu hỏi này trong hệ thống.}")
    private String fallbackAnswer;

    @Value("${rag.system-instruction}")
    private String systemInstruction;

    public ChatResponse chatWithKnowledge(String courseId, String userMessage) {
        return chatWithKnowledge(courseId, userMessage, userMessage, Collections.emptyList());
    }

    public ChatResponse chatWithKnowledge(String courseId, String originalMessage, String rewrittenMessage, List<ChatMessage> history) {
        log.info("🔍 Bắt đầu truy vấn RAG cho Course ID: [{}], Original: [{}], Rewritten: [{}]", courseId, originalMessage, rewrittenMessage);

        // 1. Quét tìm kiếm song song: Vector Search + Full-Text Search dùng câu hỏi đã viết lại (rewrittenMessage)
        List<SearchResultDto> vectorResults = new ArrayList<>();
        try {
            List<Double> queryEmbedding = geminiService.getEmbedding(rewrittenMessage);
            String embeddingString = geminiService.convertToVectorString(queryEmbedding);
            List<Object[]> rawVector = documentChunkRepository.vectorSearch(courseId, embeddingString, searchLimit);
            vectorResults = mapToSearchResults(rawVector);
            log.info("➡️ Vector Search trả về {} kết quả.", vectorResults.size());
        } catch (Exception e) {
            log.error("⚠️ Lỗi trong quá trình quét Vector Search: {}", e.getMessage());
        }

        List<SearchResultDto> ftsResults = new ArrayList<>();
        try {
            List<Object[]> rawFts = documentChunkRepository.ftsSearch(courseId, rewrittenMessage, searchLimit);
            ftsResults = mapToSearchResults(rawFts);
            log.info("➡️ Full-Text Search trả về {} kết quả.", ftsResults.size());
        } catch (Exception e) {
            log.error("⚠️ Lỗi trong quá trình quét Full-Text Search: {}", e.getMessage());
        }

        // 2. Thuật toán trộn Reciprocal Rank Fusion (RRF) & Reranking
        List<SearchResultDto> finalContext;
        if (cohereRerankService.isEnabled()) {
            List<SearchResultDto> candidates = performRRF(vectorResults, ftsResults, searchLimit);
            finalContext = cohereRerankService.rerank(rewrittenMessage, candidates, rerankTopK);
        } else {
            finalContext = performRRF(vectorResults, ftsResults, topKContext);
        }
        log.info("➡️ Đã chọn lọc thành công {} cụm ngữ cảnh gửi LLM.", finalContext.size());

        if (finalContext.isEmpty()) {
            log.warn("⚠️ Không tìm thấy bất kỳ tài liệu liên quan nào trong DB cho Course ID: {}", courseId);
            return ChatResponse.builder()
                    .answer(fallbackAnswer)
                    .citations(Collections.emptyList())
                    .build();
        }

        // 3. Xây dựng System Instruction có Guardrails chống ảo tưởng & System Prompt kèm lịch sử hội thoại
        String systemInstruction = buildSystemInstruction();
        String userPrompt = buildUserPrompt(originalMessage, finalContext, history);

        // 4. Gọi LLM sinh câu trả lời
        String rawAnswer = geminiService.generateResponse(systemInstruction, userPrompt);

        // 5. Chuẩn bị danh sách Trích dẫn (Citations) gửi kèm phản hồi
        List<Citation> citations = finalContext.stream()
                .map(chunk -> Citation.builder()
                        .lessonId(chunk.getLessonId())
                        .contentType(chunk.getContentType())
                        .sourceCitation(chunk.getSourceCitation())
                        .build())
                .distinct() // Loại bỏ các trích dẫn trùng lặp
                .collect(Collectors.toList());

        return ChatResponse.builder()
                .answer(rawAnswer)
                .citations(citations)
                .build();
    }

    /**
     * Thuật toán Reciprocal Rank Fusion (RRF) để xếp hạng lại
     */
    private List<SearchResultDto> performRRF(List<SearchResultDto> vectorResults, List<SearchResultDto> ftsResults, int limit) {
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, SearchResultDto> documentMap = new HashMap<>();

        // Tính điểm RRF cho Vector Search
        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResultDto doc = vectorResults.get(i);
            double rank = i + 1.0;
            rrfScores.put(doc.getId(), rrfScores.getOrDefault(doc.getId(), 0.0) + (1.0 / (rrfK + rank)));
            documentMap.putIfAbsent(doc.getId(), doc);
        }

        // Tính điểm RRF cho Full-Text Search
        for (int i = 0; i < ftsResults.size(); i++) {
            SearchResultDto doc = ftsResults.get(i);
            double rank = i + 1.0;
            rrfScores.put(doc.getId(), rrfScores.getOrDefault(doc.getId(), 0.0) + (1.0 / (rrfK + rank)));
            documentMap.putIfAbsent(doc.getId(), doc);
        }

        // Sắp xếp giảm dần theo điểm RRF và lấy tối đa limit
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> documentMap.get(entry.getKey()))
                .collect(Collectors.toList());
    }

    /**
     * Map dữ liệu Object[] từ Native SQL sang SearchResultDto
     */
    private List<SearchResultDto> mapToSearchResults(List<Object[]> rawResults) {
        List<SearchResultDto> results = new ArrayList<>();
        if (rawResults == null) return results;
        
        for (Object[] row : rawResults) {
            results.add(SearchResultDto.builder()
                    .id((String) row[0])
                    .courseId((String) row[1])
                    .lessonId((String) row[2])
                    .mediaId((String) row[3])
                    .chunkIndex(row[4] != null ? ((Number) row[4]).intValue() : null)
                    .content((String) row[5])
                    .contentType(row[6] != null ? ContentType.valueOf(row[6].toString().toUpperCase()) : null)
                    .sourceCitation((String) row[7])
                    .score(row[8] != null ? ((Number) row[8]).doubleValue() : 0.0)
                    .build());
        }
        return results;
    }

    /**
     * Build system instruction with strict guardrails
     */
    private String buildSystemInstruction() {
        return this.systemInstruction;
    }

    /**
     * Build user prompt combining the context chunks, query, and chat history
     */
    private String buildUserPrompt(String userMessage, List<SearchResultDto> contextChunks, List<ChatMessage> history) {
        StringBuilder promptBuilder = new StringBuilder();

        // 1. Thêm lịch sử hội thoại nếu có
        if (history != null && !history.isEmpty()) {
            promptBuilder.append("--- LỊCH SỬ HỘI THOẠI GẦN ĐÂY ---\n");
            for (ChatMessage msg : history) {
                promptBuilder.append(String.format("[%s]: %s\n", msg.getRole(), msg.getContent()));
            }
            promptBuilder.append("\n");
        }

        // 2. Thêm ngữ cảnh tài liệu khóa học
        promptBuilder.append("--- NGỮ CẢNH (CONTEXT) TÀI LIỆU KHÓA HỌC ---\n");
        for (int i = 0; i < contextChunks.size(); i++) {
            SearchResultDto chunk = contextChunks.get(i);
            promptBuilder.append(String.format("[%d] Bài học: %s | Loại: %s | Nguồn: %s\n", 
                    i + 1, chunk.getLessonId(), chunk.getContentType(), chunk.getSourceCitation()));
            
            String contextText = chunk.getContent();
            if (chunk.getMediaId() != null && chunk.getChunkIndex() != null) {
                try {
                    List<String> neighbors = documentChunkRepository.findNeighboringChunks(chunk.getMediaId(), chunk.getChunkIndex(), contextWindowSize);
                    if (neighbors != null && !neighbors.isEmpty()) {
                        contextText = String.join(" ", neighbors);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Không thể lấy cửa sổ ngữ cảnh mở rộng cho chunk {}: {}", chunk.getId(), e.getMessage());
                }
            }
            promptBuilder.append("Nội dung: ").append(contextText).append("\n\n");
        }
        
        // 3. Thêm câu hỏi mới nhất
        promptBuilder.append("--- CÂU HỎI MỚI NHẤT CỦA NGƯỜI DÙNG ---\n");
        promptBuilder.append(userMessage).append("\n\n");
        promptBuilder.append("Trả lời:");
        
        return promptBuilder.toString();
    }

    private String buildUserPrompt(String userMessage, List<SearchResultDto> contextChunks) {
        return buildUserPrompt(userMessage, contextChunks, Collections.emptyList());
    }

    /**
     * Pipeline RAG dạng Streaming trả về Flux ServerSentEvent
     */
    public Flux<ServerSentEvent<String>> chatStream(
            String courseId,
            String originalMessage,
            String rewrittenMessage,
            List<ChatMessage> history,
            String sessionId,
            ChatSessionService chatSessionService) {
        
        log.info("🔍 Bắt đầu streaming RAG cho Course ID: [{}], Session: {}", courseId, sessionId);

        // 1. Quét tìm kiếm song song sử dụng câu hỏi đã viết lại (rewrittenMessage)
        List<SearchResultDto> vectorResults = new ArrayList<>();
        try {
            List<Double> queryEmbedding = geminiService.getEmbedding(rewrittenMessage);
            String embeddingString = geminiService.convertToVectorString(queryEmbedding);
            List<Object[]> rawVector = documentChunkRepository.vectorSearch(courseId, embeddingString, searchLimit);
            vectorResults = mapToSearchResults(rawVector);
        } catch (Exception e) {
            log.error("⚠️ Lỗi trong quá trình quét Vector Search: {}", e.getMessage());
        }

        List<SearchResultDto> ftsResults = new ArrayList<>();
        try {
            List<Object[]> rawFts = documentChunkRepository.ftsSearch(courseId, rewrittenMessage, searchLimit);
            ftsResults = mapToSearchResults(rawFts);
        } catch (Exception e) {
            log.error("⚠️ Lỗi trong quá trình quét Full-Text Search: {}", e.getMessage());
        }

        // 2. Trộn RRF và lọc (Có thể Rerank)
        List<SearchResultDto> finalContext;
        if (cohereRerankService.isEnabled()) {
            List<SearchResultDto> candidates = performRRF(vectorResults, ftsResults, searchLimit);
            finalContext = cohereRerankService.rerank(rewrittenMessage, candidates, rerankTopK);
        } else {
            finalContext = performRRF(vectorResults, ftsResults, topKContext);
        }

        if (finalContext.isEmpty()) {
            return Flux.just(
                ServerSentEvent.<String>builder()
                    .event("message")
                    .data(fallbackAnswer)
                    .build()
            );
        }

        // 3. Xây dựng System Instruction & Prompt
        String systemInstruction = buildSystemInstruction();
        String userPrompt = buildUserPrompt(originalMessage, finalContext, history);

        // 4. Lấy Citations
        List<Citation> citations = finalContext.stream()
                .map(chunk -> Citation.builder()
                        .lessonId(chunk.getLessonId())
                        .contentType(chunk.getContentType())
                        .sourceCitation(chunk.getSourceCitation())
                        .build())
                .distinct()
                .collect(Collectors.toList());

        // Tạo JSON metadata sự kiện đầu tiên
        String metadataJson = "{}";
        try {
            Map<String, Object> metaMap = new HashMap<>();
            metaMap.put("sessionId", sessionId);
            metaMap.put("citations", citations);
            metadataJson = objectMapper.writeValueAsString(metaMap);
        } catch (Exception e) {
            log.error("⚠️ Lỗi serialize metadata cho stream: {}", e.getMessage());
        }

        ServerSentEvent<String> metadataEvent = ServerSentEvent.<String>builder()
                .event("metadata")
                .data(metadataJson)
                .build();

        StringBuilder fullAnswer = new StringBuilder();

        // 5. Phát metadata trước, sau đó phát text chunks từ Gemini
        Flux<ServerSentEvent<String>> contentFlux = geminiService.generateStreamingResponse(systemInstruction, userPrompt)
                .doOnNext(fullAnswer::append)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(chunk)
                        .build());

        return Flux.concat(Flux.just(metadataEvent), contentFlux)
                .doOnComplete(() -> {
                    try {
                        log.info("💾 Stream kết thúc. Tiến hành lưu vào DB và Semantic Cache cho session: {}", sessionId);
                        
                        // Lưu user message
                        chatSessionService.saveMessage(sessionId, "user", originalMessage, null);
                        
                        // Lưu assistant message
                        String citationsJson = null;
                        try {
                            citationsJson = objectMapper.writeValueAsString(citations);
                        } catch (Exception ex) {
                            log.error("⚠️ Lỗi serialize citations: {}", ex.getMessage());
                        }
                        chatSessionService.saveMessage(sessionId, "assistant", fullAnswer.toString(), citationsJson);

                        // Lưu vào Semantic Cache
                        ChatResponse responseObj = ChatResponse.builder()
                                .answer(fullAnswer.toString())
                                .citations(citations)
                                .build();
                        semanticCacheService.saveToCache(courseId, rewrittenMessage, responseObj);
                    } catch (Exception e) {
                        log.error("❌ Lỗi lưu hội thoại/cache khi hoàn thành stream: {}", e.getMessage(), e);
                    }
                });
    }
}
