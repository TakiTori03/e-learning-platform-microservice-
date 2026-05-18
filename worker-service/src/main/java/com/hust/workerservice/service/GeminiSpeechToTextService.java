// package com.hust.workerservice.service;

// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import jakarta.annotation.PreDestroy;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpEntity;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;

// import java.io.File;
// import java.io.IOException;
// import java.nio.file.Files;
// import java.util.ArrayList;
// import java.util.Base64;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.UUID;
// import java.util.concurrent.CompletableFuture;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.Semaphore;
// import java.util.concurrent.TimeUnit;

// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class GeminiSpeechToTextService {

//     @Value("${app.gemini.api-key:}")
//     private String apiKey;

//     @Value("${app.ffmpeg.path:ffmpeg}")
//     private String ffmpegPath;

//     @Value("${app.gemini.chunk-duration-seconds:300}")
//     private int chunkDurationSeconds;

//     @Value("${app.gemini.max-attempts:10}")
//     private int geminiMaxAttempts;

//     private final RestTemplate restTemplate = new RestTemplate();
//     private final ObjectMapper objectMapper = new ObjectMapper();

//     // High-performance thread pool dedicated to parallel AI transcriber workers
//     private final ExecutorService executorService = Executors.newFixedThreadPool(8);

//     public String transcribeAudioToVtt(File audioFile) {
//         if (apiKey == null || apiKey.trim().isEmpty()) {
//             throw new IllegalArgumentException("❌ Gemini API Key is missing. Please configure app.gemini.api-key.");
//         }

//         log.info("🎙️ Initiating PARALLEL Audio Chunking Pipeline for {} using Gemini 2.5 Flash...", audioFile.getName());

//         File tempDir = new File(audioFile.getParentFile(), "chunks_" + UUID.randomUUID().toString());
//         if (!tempDir.exists()) {
//             tempDir.mkdirs();
//         }

//         try {
//             // 1. Slice audio file into configured chunks using FFmpeg segment copy (instantaneous)
//             String outputPattern = new File(tempDir, "chunk_%03d.mp3").getAbsolutePath();
//             ProcessBuilder pb = new ProcessBuilder(
//                     ffmpegPath,
//                     "-i", audioFile.getAbsolutePath(),
//                     "-f", "segment",
//                     "-segment_time", String.valueOf(chunkDurationSeconds),
//                     "-c", "copy",
//                     outputPattern
//             );
            
//             log.info("✂️ Splitting audio into {}-second segments...", chunkDurationSeconds);
//             pb.inheritIO();
//             Process process = pb.start();
//             boolean completed = process.waitFor(2, TimeUnit.MINUTES);
//             if (!completed) {
//                 process.destroyForcibly();
//                 throw new RuntimeException("FFmpeg audio chunking timed out after 2 minutes.");
//             }
//             if (process.exitValue() != 0) {
//                 throw new RuntimeException("FFmpeg segmenting failed with exit code: " + process.exitValue());
//             }

//             File[] chunkFiles = tempDir.listFiles((dir, name) -> name.startsWith("chunk_") && name.endsWith(".mp3"));
//             if (chunkFiles == null || chunkFiles.length == 0) {
//                 log.warn("⚠️ No chunk files generated. Transcribing original file directly.");
//                 chunkFiles = new File[]{audioFile};
//             } else {
//                 java.util.Arrays.sort(chunkFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));
//             }

//             int totalChunks = chunkFiles.length;
//             log.info("📦 Sliced audio into {} chunks. Starting PARALLEL transcription pipeline...", totalChunks);

//             // Semaphore to limit concurrent Google uploads (protects outbound network bandwidth & Free Tier rate limits)
//             Semaphore semaphore = new Semaphore(1);
//             List<CompletableFuture<String>> futures = new ArrayList<>();

//             for (int i = 0; i < totalChunks; i++) {
//                 final int chunkIndex = i;
//                 final File chunkFile = chunkFiles[i];

//                 CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
//                     try {
//                         semaphore.acquire();
//                         log.info("🚀 [Parallel worker] Processing chunk {}/{} ({}) started", chunkIndex + 1, totalChunks, chunkFile.getName());
//                         long start = System.currentTimeMillis();
//                         String result = transcribeSingleChunk(chunkFile);
//                         long duration = System.currentTimeMillis() - start;
//                         log.info("✅ [Parallel worker] Finished chunk {}/{} in {} ms", chunkIndex + 1, totalChunks, duration);
//                         return result;
//                     } catch (InterruptedException e) {
//                         Thread.currentThread().interrupt();
//                         throw new RuntimeException("Chunk worker interrupted", e);
//                     } finally {
//                         semaphore.release();
//                     }
//                 }, executorService);

//                 futures.add(future);
//             }

//             // Wait for all chunks to be processed concurrently
//             CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

//             log.info("📝 All parallel workers completed! Merging and stitching subtitles sequentially...");

//             StringBuilder combinedVtt = new StringBuilder("WEBVTT\n\n");
//             int globalSubtitleIndex = 1;

//             for (int i = 0; i < totalChunks; i++) {
//                 String chunkRawVtt = futures.get(i).join(); // Instantaneous since all finished
//                 if (chunkRawVtt == null || chunkRawVtt.trim().isEmpty()) {
//                     continue;
//                 }

//                 long offsetMillis = (long) i * chunkDurationSeconds * 1000;
//                 String[] lines = chunkRawVtt.split("\n");
//                 for (String line : lines) {
//                     String trimmed = line.trim();
//                     if (trimmed.equals("WEBVTT") || trimmed.isEmpty()) {
//                         continue;
//                     }
//                     if (trimmed.matches("\\d+")) {
//                         combinedVtt.append(globalSubtitleIndex++).append("\n");
//                     } else if (trimmed.contains("-->")) {
//                         String[] parts = trimmed.split("-->");
//                         if (parts.length == 2) {
//                             String start = offsetAndNormalizeTimestamp(parts[0].trim(), offsetMillis);
//                             String end = offsetAndNormalizeTimestamp(parts[1].trim(), offsetMillis);
//                             combinedVtt.append(start).append(" --> ").append(end).append("\n");
//                         } else {
//                             combinedVtt.append(line).append("\n");
//                         }
//                     } else {
//                         combinedVtt.append(line).append("\n");
//                     }
//                 }
//                 combinedVtt.append("\n");
//             }

//             log.info("✅ PARALLEL Pipeline completed! Combined VTT successfully generated.");
//             return combinedVtt.toString().trim();

//         } catch (Exception e) {
//             log.error("❌ Audio Chunking PARALLEL Pipeline failed: {}", e.getMessage(), e);
//             throw new RuntimeException("Parallel Transcription Pipeline failed: " + e.getMessage(), e);
//         } finally {
//             // Cleanup temp directory recursively
//             if (tempDir.exists()) {
//                 org.springframework.util.FileSystemUtils.deleteRecursively(tempDir);
//                 log.info("🧹 Cleaned up temporary audio chunk directory: {}", tempDir.getAbsolutePath());
//             }
//         }
//     }

//     private String transcribeSingleChunk(File chunkFile) {
//         byte[] fileContent;
//         try {
//             fileContent = Files.readAllBytes(chunkFile.toPath());
//         } catch (IOException e) {
//             throw new RuntimeException("Failed to read chunk file: " + chunkFile.getName(), e);
//         }
//         String base64Audio = Base64.getEncoder().encodeToString(fileContent);

//         // Build the Gemini API endpoint
//         String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

//         // Construct JSON Payload
//         Map<String, Object> payload = new HashMap<>();
        
//         // Set temperature to 0.0 for maximum determinism
//         Map<String, Object> generationConfig = new HashMap<>();
//         generationConfig.put("temperature", 0.0);
//         payload.put("generationConfig", generationConfig);
        
//         Map<String, Object> inlineData = new HashMap<>();
//         inlineData.put("mimeType", "audio/mp3");
//         inlineData.put("data", base64Audio);

//         Map<String, Object> audioPart = new HashMap<>();
//         audioPart.put("inlineData", inlineData);

//         Map<String, Object> textPart = new HashMap<>();
//         textPart.put("text", 
//             "Bạn là một chuyên gia bóc băng âm thanh chuyên nghiệp trong lĩnh vực Công nghệ thông tin. Nhiệm vụ của bạn là lắng nghe file âm thanh bài giảng này, tự động nhận diện ngôn ngữ nói được sử dụng (chỉ có thể là tiếng Anh hoặc tiếng Việt), và thực hiện bóc tách (Speech-to-Text) một cách **chính xác từng từ một (word-for-word verbatim)** bằng chính ngôn ngữ đó.\n\n" +
//             "Quy tắc nghiêm ngặt:\n" +
//             "1. BẮT ĐẦU BÓC BĂNG TỪ GIÂY ĐẦU TIÊN (00:00:00.000) của file âm thanh cho đến khi kết thúc toàn bộ file. Tuyệt đối không được bỏ qua phần đầu hoặc nhảy cóc sang giữa video.\n" +
//             "2. TUYỆT ĐỐI KHÔNG TÓM TẮT, không diễn đạt lại (paraphrase), không lược bỏ bất kỳ từ hay câu nói nào của giảng viên. Phải phiên âm đầy đủ 100% nội dung được nói.\n" +
//             "3. CHIA NHỎ các mốc thời gian chi tiết: Mỗi phân đoạn phụ đề chỉ nên dài từ 3 đến 8 giây để khớp hoàn hảo với giọng nói. Tuyệt đối không gộp nhiều câu nói dài liên tục quá 10 giây vào một phân đoạn.\n" +
//             "4. ĐỊNH DẠNG MỐC THỜI GIAN CHUẨN: Mốc thời gian bắt buộc phải viết đầy đủ dạng 'HH:MM:SS.mmm --> HH:MM:SS.mmm' ở cả thời điểm bắt đầu và kết thúc (Ví dụ: '00:00:01.320 --> 00:00:05.480'). Tuyệt đối không viết tắt phần giờ, phần phút hay viết sai mốc thời gian.\n" +
//             "5. ĐỊNH DẠNG ĐẦU RA WebVTT:\n" +
//             "   Bắt đầu bằng dòng 'WEBVTT' ở dòng đầu tiên.\n" +
//             "   Mỗi phân đoạn gồm:\n" +
//             "   NUMBER\n" +
//             "   HH:MM:SS.mmm --> HH:MM:SS.mmm\n" +
//             "   [Nội dung phiên âm chính xác từng từ]\n\n" +
//             "6. XỬ LÝ KHOẢNG LẶNG & TẠP ÂM: Nếu có khoảng lặng dài (giảng viên dừng nói) hoặc tạp âm (tiếng click chuột, gõ phím), hãy bỏ qua khoảng thời gian đó, tuyệt đối không tự bịa từ (hallucination).\n" +
//             "7. THUẬT NGỮ CHUYÊN NGÀNH: Hãy ưu tiên nhận diện và viết đúng các thuật ngữ công nghệ thông tin (Ví dụ: Java, Spring Boot, Microservices, Kafka, Docker, Kubernetes, HLS, CS2, skin, etc.) dựa trên ngữ cảnh phát âm.\n" +
//             "8. CHỈ TRẢ VỀ nội dung tệp tin WebVTT thuần túy, không thêm bất kỳ văn bản giới thiệu nào khác và không bao bọc bởi markdown code blocks (như ```vtt)."
//         );

//         Map<String, Object> partsMap = new HashMap<>();
//         partsMap.put("parts", List.of(audioPart, textPart));

//         payload.put("contents", List.of(partsMap));

//         // Send POST request
//         HttpHeaders headers = new HttpHeaders();
//         headers.setContentType(MediaType.APPLICATION_JSON);
//         HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

//         int maxAttempts = this.geminiMaxAttempts;
//         int attempt = 0;
//         long backoffMillis = 15000; // Start with 15 seconds

//         while (attempt < maxAttempts) {
//             try {
//                 attempt++;
//                 log.info("🛰️ Sending chunk {} to Gemini API (Attempt {}/{})", chunkFile.getName(), attempt, maxAttempts);
//                 String responseStr = restTemplate.postForObject(url, entity, String.class);
                
//                 // Parse Response JSON
//                 JsonNode root = objectMapper.readTree(responseStr);
//                 JsonNode candidate = root.path("candidates").get(0);
//                 if (candidate != null) {
//                     String responseText = candidate.path("content").path("parts").get(0).path("text").asText();
//                     if (responseText != null && !responseText.trim().isEmpty()) {
//                         responseText = responseText.replace("```vtt", "")
//                                                    .replace("```", "")
//                                                    .trim();
//                         return responseText;
//                     }
//                 }
//                 throw new RuntimeException("Empty response from Gemini API");

//             } catch (org.springframework.web.client.HttpStatusCodeException e) {
//                 if (e.getStatusCode().value() == 429 || e.getStatusCode().value() == 503) {
//                     long waitTime = backoffMillis;
                    
//                     if (e.getStatusCode().value() == 429) {
//                         try {
//                             String errorBody = e.getResponseBodyAsString();
//                             JsonNode errRoot = objectMapper.readTree(errorBody);
//                             JsonNode details = errRoot.path("error").path("details");
//                             for (JsonNode detail : details) {
//                                 if (detail.has("retryDelay")) {
//                                     String delayStr = detail.get("retryDelay").asText(); // e.g. "41s" or "41.362s"
//                                     String numeric = delayStr.replaceAll("[^0-9.]", "");
//                                     if (!numeric.isEmpty()) {
//                                         double delaySec = Double.parseDouble(numeric);
//                                         waitTime = (long) (delaySec * 1000) + 2000; // Add 2s padding
//                                         log.info("⏰ Extracted explicit Google retry delay: {} ms for chunk {}", waitTime, chunkFile.getName());
//                                     }
//                                 }
//                             }
//                         } catch (Exception parseEx) {
//                             log.warn("⚠️ Failed to parse Google 429 retry delay. Using default backoff: {} ms", waitTime);
//                         }
//                         log.warn("⚠️ Rate Limit (429) hit on chunk {} (attempt {}/{}). Google requested wait. Sleeping for {} ms...", 
//                                 chunkFile.getName(), attempt, maxAttempts, waitTime);
//                     } else {
//                         log.warn("⚠️ Google API returned 503 Service Unavailable for chunk {} (attempt {}/{}). Sleeping for {} ms...", 
//                                 chunkFile.getName(), attempt, maxAttempts, waitTime);
//                     }

//                     if (attempt >= maxAttempts) {
//                         throw new RuntimeException("Exceeded maximum retry attempts for Gemini API rate limits", e);
//                     }

//                     try {
//                         Thread.sleep(waitTime);
//                     } catch (InterruptedException ie) {
//                         Thread.currentThread().interrupt();
//                         throw new RuntimeException("Retry wait interrupted", ie);
//                     }
//                     backoffMillis *= 2; // Exponential backoff fallback

//                 } else {
//                     log.error("❌ HTTP Error occurred: {}", e.getMessage(), e);
//                     throw new RuntimeException("Chunk transcription failed: " + e.getMessage(), e);
//                 }
//             } catch (Exception e) {
//                 log.error("❌ Failed to transcribe single chunk using Gemini API: {}", e.getMessage(), e);
//                 throw new RuntimeException("Chunk transcription failed: " + e.getMessage(), e);
//             }
//         }
//         throw new RuntimeException("Failed to transcribe chunk after maximum retries");
//     }

//     private String offsetAndNormalizeTimestamp(String ts, long offsetMillis) {
//         String clean = ts.replaceAll("[^0-9]", ":");
//         String[] tokens = clean.split(":");
        
//         int hours = 0;
//         int minutes = 0;
//         int seconds = 0;
//         int millis = 0;
        
//         try {
//             if (tokens.length >= 4) {
//                 hours = Integer.parseInt(tokens[tokens.length - 4]);
//                 minutes = Integer.parseInt(tokens[tokens.length - 3]);
//                 seconds = Integer.parseInt(tokens[tokens.length - 2]);
//                 millis = Integer.parseInt(tokens[tokens.length - 1]);
//             } else if (tokens.length == 3) {
//                 minutes = Integer.parseInt(tokens[tokens.length - 3]);
//                 seconds = Integer.parseInt(tokens[tokens.length - 2]);
//                 millis = Integer.parseInt(tokens[tokens.length - 1]);
//             } else if (tokens.length == 2) {
//                 seconds = Integer.parseInt(tokens[tokens.length - 2]);
//                 millis = Integer.parseInt(tokens[tokens.length - 1]);
//             }
//         } catch (Exception e) {
//             return ts; // Fallback
//         }
        
//         // Convert all to total milliseconds
//         long totalMillis = ((long)hours * 3600 + (long)minutes * 60 + seconds) * 1000 + millis;
//         totalMillis += offsetMillis;
        
//         // Convert back to HH:MM:SS.mmm
//         long newHours = totalMillis / 3600000;
//         long remainder = totalMillis % 3600000;
//         long newMinutes = remainder / 60000;
//         remainder = remainder % 60000;
//         long newSeconds = remainder / 1000;
//         long newMillis = remainder % 1000;
        
//         return String.format("%02d:%02d:%02d.%03d", newHours, newMinutes, newSeconds, newMillis);
//     }

//     @PreDestroy
//     public void shutdownExecutor() {
//         log.info("🔌 Shutting down Gemini STT Executor Service...");
//         executorService.shutdown();
//         try {
//             if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
//                 executorService.shutdownNow();
//             }
//         } catch (InterruptedException e) {
//             executorService.shutdownNow();
//             Thread.currentThread().interrupt();
//         }
//     }
// }
