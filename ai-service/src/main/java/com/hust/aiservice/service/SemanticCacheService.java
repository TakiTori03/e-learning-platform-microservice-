package com.hust.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.aiservice.dto.ChatResponse;
import com.hust.aiservice.dto.Citation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticCacheService {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final GeminiService geminiService;

    @Value("${rag.redis-exact-prefix:cache:exact:}")
    private String redisExactPrefix;

    @Value("${rag.cache-ttl-hours:24}")
    private long cacheTtlHours;

    @Value("${rag.semantic-cache-threshold:0.05}")
    private double semanticCacheThreshold;

    /**
     * Tra cứu cache: Đầu tiên tìm chính xác trên Redis (L1), sau đó tìm tương đồng trên Postgres (L2)
     */
    public ChatResponse getCachedResponse(String courseId, String query) {
        // 1. Level 1: Exact Match in Redis
        String exactKey = redisExactPrefix + courseId + ":" + query.trim().toLowerCase();
        try {
            String cachedJson = redisTemplate.opsForValue().get(exactKey);
            if (cachedJson != null) {
                log.info("🚀 [L1 Cache Hit] Redis exact match for query: {}", query);
                return deserializeResponse(cachedJson);
            }
        } catch (Exception e) {
            log.warn("⚠️ Lỗi truy vấn L1 Cache (Redis): {}", e.getMessage());
        }

        // 2. Level 2: Semantic Match in Postgres using pgvector
        try {
            List<Double> queryEmbedding = geminiService.getEmbedding(query);
            String embeddingString = geminiService.convertToVectorString(queryEmbedding);

            // Tìm câu hỏi có khoảng cách Cosine <= threshold
            String sql = "SELECT id, query, answer, citations, (embedding <=> cast(? as vector)) as distance " +
                         "FROM semantic_cache " +
                         "WHERE course_id = ? " +
                         "AND (embedding <=> cast(? as vector)) <= ? " +
                         "ORDER BY distance ASC LIMIT 1";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, embeddingString, courseId, embeddingString, semanticCacheThreshold);

            if (!results.isEmpty()) {
                Map<String, Object> row = results.get(0);
                Long cacheId = ((Number) row.get("id")).longValue();
                String cachedQuery = (String) row.get("query");
                String answer = (String) row.get("answer");
                String citationsJson = (String) row.get("citations");
                double distance = ((Number) row.get("distance")).doubleValue();

                log.info("🚀 [L2 Cache Hit] Postgres semantic match (similarity={}%): '{}' matched with '{}'",
                        String.format("%.1f", (1.0 - distance) * 100), query, cachedQuery);

                try {
                    jdbcTemplate.update("UPDATE semantic_cache SET hits_count = hits_count + 1, last_accessed_at = NOW() WHERE id = ?", cacheId);
                } catch (Exception ex) {
                    log.warn("⚠️ Không thể cập nhật lượt truy cập cache: {}", ex.getMessage());
                }

                List<Citation> citations = Collections.emptyList();
                if (citationsJson != null) {
                    try {
                        citations = objectMapper.readValue(citationsJson,
                                objectMapper.getTypeFactory().constructCollectionType(List.class, Citation.class));
                    } catch (Exception e) {
                        log.error("⚠️ Lỗi deserialize citations từ L2 cache: {}", e.getMessage());
                    }
                }

                ChatResponse response = ChatResponse.builder()
                        .answer(answer)
                        .citations(citations)
                        .build();

                // Lưu ngược lại L1 (Redis) để lần sau truy cập nhanh hơn
                saveToL1(exactKey, response);

                return response;
            }
        } catch (Exception e) {
            log.warn("⚠️ Lỗi truy vấn L2 Cache (Postgres): {}", e.getMessage());
        }

        return null; // Cache Miss
    }

    /**
     * Lưu kết quả mới vào cả L1 (Redis) và L2 (Postgres)
     */
    public void saveToCache(String courseId, String query, ChatResponse response) {
        if (response == null || response.getAnswer() == null) {
            return;
        }

        String exactKey = redisExactPrefix + courseId + ":" + query.trim().toLowerCase();

        // 1. Lưu L1 Redis
        saveToL1(exactKey, response);

        // 2. Lưu L2 Postgres
        try {
            List<Double> queryEmbedding = geminiService.getEmbedding(query);
            String embeddingString = geminiService.convertToVectorString(queryEmbedding);
            String citationsJson = objectMapper.writeValueAsString(response.getCitations());

            String sql = "INSERT INTO semantic_cache (course_id, query, answer, citations, embedding, embedding_model, hits_count, last_accessed_at) " +
                         "VALUES (?, ?, ?, cast(? as jsonb), cast(? as vector), ?, 1, NOW())";

            jdbcTemplate.update(sql, courseId, query, response.getAnswer(), citationsJson, embeddingString, geminiService.getEmbeddingModel());
            log.info("💾 Đã lưu Q&A vào L2 Cache (Postgres) cho câu hỏi: '{}'", query);
        } catch (Exception e) {
            log.warn("⚠️ Không thể lưu Q&A vào L2 Cache (Postgres): {}", e.getMessage());
        }
    }

    private void saveToL1(String key, ChatResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, cacheTtlHours, TimeUnit.HOURS);
            log.info("💾 Đã lưu Q&A vào L1 Cache (Redis)");
        } catch (Exception e) {
            log.warn("⚠️ Không thể lưu Q&A vào L1 Cache (Redis): {}", e.getMessage());
        }
    }

    private ChatResponse deserializeResponse(String json) {
        try {
            return objectMapper.readValue(json, ChatResponse.class);
        } catch (Exception e) {
            log.error("⚠️ Lỗi deserialize ChatResponse: {}", e.getMessage());
            return null;
        }
    }
}
