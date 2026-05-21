package com.hust.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        
        // Cấu hình timeout để tránh treo luồng vô hạn khi gọi API ngoài
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(15000); // 15 giây kết nối
        requestFactory.setReadTimeout(60000);    // 60 giây đọc dữ liệu
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.embedding-model}")
    private String embeddingModel;

    @Value("${gemini.chat-model}")
    private String chatModel;

    /**
     * Lấy vector embedding (768 chiều) cho đoạn text
     */
    public List<Double> getEmbedding(String text) {
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent?key=%s", 
                                   embeddingModel, apiKey);

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
            return values;
        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy embedding từ Gemini API: {}", e.getMessage());
            throw new RuntimeException("Lỗi sinh embedding từ Gemini: " + e.getMessage(), e);
        }
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
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", 
                                   chatModel, apiKey);

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
            generationConfig.put("temperature", 0.2);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(rootNode.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            
            // Kiểm tra các trường hợp Null hoặc cấu trúc bị lỗi từ API
            JsonNode candidates = responseJson.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                return textNode.asText();
            }
            
            log.warn("⚠️ API không trả về candidate hợp lệ: {}", response.getBody());
            return "Không thể sinh câu trả lời do cấu trúc API rỗng.";
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi Gemini API Chat: {}", e.getMessage());
            throw new RuntimeException("Lỗi sinh câu trả lời từ Gemini: " + e.getMessage(), e);
        }
    }
}
