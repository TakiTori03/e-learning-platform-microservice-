package com.hust.workerservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hust.commonlibrary.annotation.TrackPerformance;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WhisperLocalSpeechToTextService {

    @Value("${app.whisper.enabled:true}")
    private boolean enabled;

    @Value("${app.whisper.path:bin/whisper/whisper-cli.exe}")
    private String whisperPath;

    @Value("${app.whisper.model-path:models/whisper/ggml-base.bin}")
    private String modelPath;

    @Value("${app.whisper.language:auto}")
    private String language;


    @TrackPerformance(threshold = 5000, description = "Whisper Local Speech-To-Text Service")
    public String transcribeAudioToVtt(File audioFile) {
        if (!enabled) {
            throw new IllegalStateException("❌ Whisper Local STT is disabled in configuration.");
        }

        log.info("🎙️ Bắt đầu tiến trình bóc băng Offline bằng Whisper.cpp cho file: {}", audioFile.getName());

        File tempDir = new File(audioFile.getParentFile(), "whisper_" + UUID.randomUUID());
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File outputVttFile = new File(tempDir, "transcript.vtt");

        try {
            // --- BƯỚC 1: ĐÃ LOẠI BỎ HOÀN TOÀN (Vì file đầu vào đã là WAV 16kHz Mono từ VideoProcessingService) ---

            File whisperExecutable = new File(whisperPath);
            if (!whisperExecutable.exists()) {
                whisperExecutable = new File("worker-service/" + whisperPath);
            }
            if (!whisperExecutable.exists()) {
                throw new RuntimeException("Không tìm thấy file thực thi Whisper.");
            }

            File model = new File(modelPath);
            if (!model.exists()) {
                model = new File("worker-service/" + modelPath);
            }
            if (!model.exists()) {
                throw new RuntimeException("Không tìm thấy file Model Whisper.");
            }

            log.info("🚀 Đang chạy xử lý nơ-ron cục bộ bằng Whisper.cpp (Model: {})...", model.getAbsolutePath());
            ProcessBuilder pbWhisper = new ProcessBuilder(
                    whisperExecutable.getAbsolutePath(),
                    "-m", model.getAbsolutePath(),
                    "-f", audioFile.getAbsolutePath(), // Đọc trực tiếp audioFile gốc
                    "-l", language,
                    "-t", "4",  // 💡 TỐI ƯU: Thêm tham số -t 4 hoặc 6 để tận dụng số core CPU bổ trợ nạp dữ liệu vào GPU nhanh hơn
                    "-ovtt",
                    "-of", outputVttFile.getAbsolutePath().replace(".vtt", "")
            );

            // Không dùng inheritIO để tránh nghẽn luồng khi log GPU nhảy liên tục
            pbWhisper.redirectErrorStream(true);
            Process procWhisper = pbWhisper.start();

            // Đọc giải phóng luồng output của Whisper.cpp
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(procWhisper.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Bạn có thể log tiến độ tại đây nếu muốn hoặc bỏ qua để tối ưu tốc độ
                    if(line.contains("processing") || line.contains("system_info")) {
                        log.info("[Whisper.cpp] {}", line);
                    }
                }
            }

            boolean completed = procWhisper.waitFor(15, TimeUnit.MINUTES);
            if (!completed) {
                procWhisper.destroyForcibly();
                throw new RuntimeException("Quá thời gian bóc băng (Timeout 15 phút).");
            }
            if (procWhisper.exitValue() != 0) {
                throw new RuntimeException("Whisper.cpp kết thúc với mã lỗi: " + procWhisper.exitValue());
            }

            if (!outputVttFile.exists()) {
                throw new RuntimeException("Không tìm thấy file WebVTT đầu ra.");
            }

            String vttContent = Files.readString(outputVttFile.toPath());
            log.info("✅ Bóc băng Offline thành công! Độ dài VTT: {} ký tự", vttContent.length());
            return vttContent;

        } catch (Exception e) {
            log.error("❌ Tiến trình bóc băng Whisper.cpp thất bại: {}", e.getMessage(), e);
            throw new RuntimeException("Whisper Local STT failed: " + e.getMessage(), e);
        } finally {
            FileSystemUtils.deleteRecursively(tempDir);
            log.info("🧹 Đã dọn dẹp thư mục tạm của Whisper: {}", tempDir.getAbsolutePath());
        }
    }
}
