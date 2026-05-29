package com.hust.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public GeminiService(
            ObjectMapper objectMapper,
            @Value("${gemini.connect-timeout-ms:15000}") int connectTimeoutMs,
            @Value("${gemini.read-timeout-ms:60000}") int readTimeoutMs) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
        
        // Cấu hình timeout để tránh treo luồng vô hạn khi gọi API ngoài
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs); // Kết nối
        requestFactory.setReadTimeout(readTimeoutMs);    // Đọc dữ liệu
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.embedding-model}")
    private String embeddingModel;

    @Value("${gemini.chat-model}")
    private String chatModel;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Value("${gemini.max-attempts:3}")
    private int maxAttempts;

    @Value("${gemini.backoff-delay-ms:1000}")
    private long backoffDelayMs;

    @Value("${gemini.embedding-dimensions:768}")
    private int embeddingDimensions;

    @Value("${gemini.temperature:0.2}")
    private double temperature;

    /**
     * Lấy vector embedding (768 chiều) cho đoạn text
     */
    public List<Double> getEmbedding(String text) {
        String url = String.format("%s/v1beta/models/%s:embedContent?key=%s", 
                                   baseUrl, embeddingModel, apiKey);

        long currentDelay = backoffDelayMs;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ObjectNode rootNode = objectMapper.createObjectNode();
                rootNode.put("model", "models/" + embeddingModel);
                
                ObjectNode contentNode = rootNode.putObject("content");
                contentNode.putArray("parts").addObject().put("text", text);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> entity = new HttpEntity<>(rootNode.toString(), headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                JsonNode responseJson = objectMapper.readTree(response.getBody());
                JsonNode valuesNode = responseJson.path("embedding").path("values");

                List<Double> values = new ArrayList<>();
                if (valuesNode.isArray()) {
                    for (JsonNode val : valuesNode) {
                        values.add(val.asDouble());
                    }
                }
                
                // Truncate to target dimensions
                if (values.size() > embeddingDimensions) {
                    values = new ArrayList<>(values.subList(0, embeddingDimensions));
                }
                return values;
            } catch (Exception e) {
                lastException = e;
                log.warn("⚠️ Lỗi lấy embedding từ Gemini (Lần thử {}/{}): {}", attempt, maxAttempts, e.getMessage());
                
                long currentDelayMs = currentDelay;
                if (e instanceof org.springframework.web.client.HttpStatusCodeException) {
                    org.springframework.web.client.HttpStatusCodeException hs = (org.springframework.web.client.HttpStatusCodeException) e;
                    if (hs.getStatusCode().value() == 429) {
                        String retryAfter = hs.getResponseHeaders() != null ? hs.getResponseHeaders().getFirst("Retry-After") : null;
                        if (retryAfter != null) {
                            try {
                                currentDelayMs = Long.parseLong(retryAfter) * 1000;
                                log.info("⏳ Nhận header Retry-After: Chờ {}ms từ Gemini API", currentDelayMs);
                            } catch (NumberFormatException nfe) {
                                // Bỏ qua lỗi parse
                            }
                        }
                    }
                }

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(currentDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Tiến trình bị gián đoạn khi đang chờ retry", ie);
                    }
                    currentDelay *= 2;
                }
            }
        }
        
        log.error("❌ Thất bại khi lấy embedding từ Gemini API sau {} lần thử.", maxAttempts);
        throw new RuntimeException("Lỗi sinh embedding từ Gemini sau các lần thử: " + lastException.getMessage(), lastException);
    }

    /**
     * Chuyển list double sang định dạng chuỗi của pgvector "[val1,val2,...]"
     */
    public String convertToVectorString(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return "[]";
        }
        return vector.toString();
    }

    /**
     * Gọi Gemini API để sinh câu trả lời dựa trên chỉ dẫn hệ thống và prompt
     */
    public String generateResponse(String systemInstruction, String prompt) {
        String url = String.format("%s/v1beta/models/%s:generateContent?key=%s", 
                                   baseUrl, chatModel, apiKey);

        long currentDelay = backoffDelayMs;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ObjectNode rootNode = objectMapper.createObjectNode();
                
                // Contents
                ArrayNode contentsArray = rootNode.putArray("contents");
                ObjectNode userContent = contentsArray.addObject();
                userContent.put("role", "user");
                userContent.putArray("parts").addObject().put("text", prompt);

                // System Instruction
                ObjectNode systemInstructionNode = rootNode.putObject("systemInstruction");
                systemInstructionNode.putArray("parts").addObject().put("text", systemInstruction);

                // Config
                ObjectNode generationConfig = rootNode.putObject("generationConfig");
                generationConfig.put("temperature", temperature);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> entity = new HttpEntity<>(rootNode.toString(), headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                JsonNode responseJson = objectMapper.readTree(response.getBody());
                
                JsonNode candidates = responseJson.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                    return textNode.asText();
                }
                
                log.warn("⚠️ API không trả về candidate hợp lệ: {}", response.getBody());
                return "Không thể sinh câu trả lời do cấu trúc API rỗng.";
            } catch (Exception e) {
                lastException = e;
                log.warn("⚠️ Lỗi sinh câu trả lời Chat (Lần thử {}/{}): {}", attempt, maxAttempts, e.getMessage());

                long currentDelayMs = currentDelay;
                if (e instanceof org.springframework.web.client.HttpStatusCodeException) {
                    org.springframework.web.client.HttpStatusCodeException hs = (org.springframework.web.client.HttpStatusCodeException) e;
                    if (hs.getStatusCode().value() == 429) {
                        String retryAfter = hs.getResponseHeaders() != null ? hs.getResponseHeaders().getFirst("Retry-After") : null;
                        if (retryAfter != null) {
                            try {
                                currentDelayMs = Long.parseLong(retryAfter) * 1000;
                                log.info("⏳ Nhận header Retry-After: Chờ {}ms từ Gemini API", currentDelayMs);
                            } catch (NumberFormatException nfe) {
                                // Bỏ qua lỗi parse
                            }
                        }
                    }
                }

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(currentDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Tiến trình bị gián đoạn khi đang chờ retry", ie);
                    }
                    currentDelay *= 2;
                }
            }
        }

        log.error("❌ Thất bại khi gọi Gemini API Chat sau {} lần thử.", maxAttempts);
        throw new RuntimeException("Lỗi sinh câu trả lời từ Gemini sau các lần thử: " + lastException.getMessage(), lastException);
    }

    public Flux<String> generateStreamingResponse(String systemInstruction, String prompt) {
        String url = String.format("%s/v1beta/models/%s:streamGenerateContent?alt=sse&key=%s", 
                                   baseUrl, chatModel, apiKey);

        try {
            ObjectNode rootNode = objectMapper.createObjectNode();
            
            // Contents
            ArrayNode contentsArray = rootNode.putArray("contents");
            ObjectNode userContent = contentsArray.addObject();
            userContent.put("role", "user");
            userContent.putArray("parts").addObject().put("text", prompt);

            // System Instruction
            ObjectNode systemInstructionNode = rootNode.putObject("systemInstruction");
            systemInstructionNode.putArray("parts").addObject().put("text", systemInstruction);

            // Config
            ObjectNode generationConfig = rootNode.putObject("generationConfig");
            generationConfig.put("temperature", temperature);

            String requestBody = rootNode.toString();

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .flatMap(sseLine -> {
                        try {
                            String trimmed = sseLine.trim();
                            if (trimmed.isEmpty()) {
                                return Flux.empty();
                            }
                            if (trimmed.startsWith("data:")) {
                                trimmed = trimmed.substring(5).trim();
                            }
                            
                            JsonNode responseJson = objectMapper.readTree(trimmed);
                            JsonNode candidates = responseJson.path("candidates");
                            if (candidates.isArray() && candidates.size() > 0) {
                                JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                                if (!textNode.isMissingNode() && textNode.asText() != null) {
                                    return Flux.just(textNode.asText());
                                }
                            }
                        } catch (Exception e) {
                            log.debug("⚠️ Bỏ qua dòng SSE không parse được: {}", sseLine);
                        }
                        return Flux.empty();
                    });
        } catch (Exception e) {
            log.error("❌ Lỗi khi khởi tạo yêu cầu Gemini Streaming: {}", e.getMessage());
            return Flux.error(new RuntimeException("Lỗi sinh stream từ Gemini: " + e.getMessage(), e));
        }
    }

    public String getEmbeddingModel() {
        return this.embeddingModel;
    }
}

