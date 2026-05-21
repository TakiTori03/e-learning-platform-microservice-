package com.hust.aiservice.service;

import com.hust.aiservice.dto.ChatResponse;
import com.hust.aiservice.dto.Citation;
import com.hust.aiservice.dto.SearchResultDto;
import com.hust.commonlibrary.entity.ContentType;
import com.hust.aiservice.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final GeminiService geminiService;
    private final DocumentChunkRepository documentChunkRepository;

    private static final int SEARCH_LIMIT = 20; // Lấy top 20 của mỗi engine trước khi trộn RRF
    private static final int TOP_K_CONTEXT = 5;  // Lấy top 5 chunks sau trộn RRF làm ngữ cảnh gửi LLM
    private static final double RRF_K = 60.0;    // Hằng số K trong công thức RRF

    public ChatResponse chatWithKnowledge(String courseId, String userMessage) {
        log.info("🔍 Bắt đầu truy vấn RAG cho Course ID: [{}], Question: [{}]", courseId, userMessage);

        // 1. Quét tìm kiếm song song: Vector Search + Full-Text Search
        List<SearchResultDto> vectorResults = new ArrayList<>();
        try {
            List<Double> queryEmbedding = geminiService.getEmbedding(userMessage);
            String embeddingString = geminiService.convertToVectorString(queryEmbedding);
            List<Object[]> rawVector = documentChunkRepository.vectorSearch(courseId, embeddingString, SEARCH_LIMIT);
            vectorResults = mapToSearchResults(rawVector);
            log.info("➡️ Vector Search trả về {} kết quả.", vectorResults.size());
        } catch (Exception e) {
            log.error("⚠️ Lỗi trong quá trình quét Vector Search: {}", e.getMessage());
        }

        List<SearchResultDto> ftsResults = new ArrayList<>();
        try {
            List<Object[]> rawFts = documentChunkRepository.ftsSearch(courseId, userMessage, SEARCH_LIMIT);
            ftsResults = mapToSearchResults(rawFts);
            log.info("➡️ Full-Text Search trả về {} kết quả.", ftsResults.size());
        } catch (Exception e) {
            log.error("⚠️ Lỗi trong quá trình quét Full-Text Search: {}", e.getMessage());
        }

        // 2. Thuật toán trộn Reciprocal Rank Fusion (RRF)
        List<SearchResultDto> fusedContext = performRRF(vectorResults, ftsResults);
        log.info("➡️ Đã trộn RRF thành công. Top {} chunks được chọn làm ngữ cảnh.", fusedContext.size());

        if (fusedContext.isEmpty()) {
            log.warn("⚠️ Không tìm thấy bất kỳ tài liệu liên quan nào trong DB cho Course ID: {}", courseId);
            return ChatResponse.builder()
                    .answer("Xin lỗi, tôi chưa tìm thấy tài liệu học tập nào liên quan đến câu hỏi này trong hệ thống.")
                    .citations(Collections.emptyList())
                    .build();
        }

        // 3. Xây dựng System Instruction có Guardrails chống ảo tưởng & System Prompt
        String systemInstruction = buildSystemInstruction();
        String userPrompt = buildUserPrompt(userMessage, fusedContext);

        // 4. Gọi LLM sinh câu trả lời
        String rawAnswer = geminiService.generateResponse(systemInstruction, userPrompt);

        // 5. Chuẩn bị danh sách Trích dẫn (Citations) gửi kèm phản hồi
        List<Citation> citations = fusedContext.stream()
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
    private List<SearchResultDto> performRRF(List<SearchResultDto> vectorResults, List<SearchResultDto> ftsResults) {
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, SearchResultDto> documentMap = new HashMap<>();

        // Tính điểm RRF cho Vector Search
        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResultDto doc = vectorResults.get(i);
            double rank = i + 1.0;
            rrfScores.put(doc.getId(), rrfScores.getOrDefault(doc.getId(), 0.0) + (1.0 / (RRF_K + rank)));
            documentMap.putIfAbsent(doc.getId(), doc);
        }

        // Tính điểm RRF cho Full-Text Search
        for (int i = 0; i < ftsResults.size(); i++) {
            SearchResultDto doc = ftsResults.get(i);
            double rank = i + 1.0;
            rrfScores.put(doc.getId(), rrfScores.getOrDefault(doc.getId(), 0.0) + (1.0 / (RRF_K + rank)));
            documentMap.putIfAbsent(doc.getId(), doc);
        }

        // Sắp xếp giảm dần theo điểm RRF và lấy tối đa TOP_K_CONTEXT
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(TOP_K_CONTEXT)
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
                    .content((String) row[3])
                    .contentType(row[4] != null ? ContentType.valueOf(row[4].toString().toUpperCase()) : null)
                    .sourceCitation((String) row[5])
                    .score(row[6] != null ? ((Number) row[6]).doubleValue() : 0.0)
                    .build());
        }
        return results;
    }

    /**
     * Build system instruction with strict guardrails
     */
    private String buildSystemInstruction() {
        return "Bạn là trợ lý học tập AI xuất sắc của hệ thống E-Learning.\n" +
               "NHIỆM VỤ CỦA BẠN:\n" +
               "Trả lời câu hỏi của người học một cách chính xác, học thuật, dễ hiểu dựa TRỰC TIẾP và CHỈ dựa trên phần ngữ cảnh (CONTEXT) được cung cấp dưới đây.\n\n" +
               "RÀO CẢN NGHIÊM NGẶT (ANTI-HALLUCINATION GUARDRAILS):\n" +
               "1. KHÔNG được tự bịa ra thông tin, số liệu, sự kiện hoặc đưa bất kỳ kiến thức nào nằm ngoài phần CONTEXT được cung cấp.\n" +
               "2. Nếu câu hỏi của người học KHÔNG có thông tin giải đáp trong CONTEXT, hãy trả lời chính xác như sau: \"Tôi không tìm thấy thông tin này trong tài liệu học tập của khóa học.\"\n" +
               "   Tuyệt đối KHÔNG cố trả lời bằng kiến thức chung của bạn khi CONTEXT không có.\n" +
               "3. Trả lời bằng ngôn ngữ tự nhiên, mạch lạc, dùng đúng ngôn ngữ mà người dùng hỏi (thường là Tiếng Việt).\n" +
               "4. Nêu rõ nguồn trích dẫn bằng số thứ tự hoặc tag tương ứng từ tài liệu nếu có thể.";
    }

    /**
     * Build user prompt combining the context chunks and query
     */
    private String buildUserPrompt(String userMessage, List<SearchResultDto> contextChunks) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("--- CONTEXT ---\n");
        
        for (int i = 0; i < contextChunks.size(); i++) {
            SearchResultDto chunk = contextChunks.get(i);
            promptBuilder.append(String.format("[%d] Bài học: %s | Loại: %s | Nguồn: %s\n", 
                    i + 1, chunk.getLessonId(), chunk.getContentType(), chunk.getSourceCitation()));
            promptBuilder.append("Nội dung: ").append(chunk.getContent()).append("\n\n");
        }
        
        promptBuilder.append("--- CÂU HỎI NGƯỜI DÙNG ---\n");
        promptBuilder.append(userMessage).append("\n\n");
        promptBuilder.append("Trả lời:");
        
        return promptBuilder.toString();
    }
}
