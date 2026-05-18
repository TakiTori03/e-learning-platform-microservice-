package com.hust.workerservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hust.commonlibrary.annotation.TrackPerformance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @TrackPerformance(threshold = 5000, description = "Whisper Local Speech-To-Text Service")
    public String transcribeAudioToVtt(File audioFile) {
        if (!enabled) {
            throw new IllegalStateException("❌ Whisper Local STT is disabled in configuration.");
        }

        log.info("🎙️ Bắt đầu tiến trình bóc băng Offline bằng Whisper.cpp cho file: {}", audioFile.getName());
        
        File tempDir = new File(audioFile.getParentFile(), "whisper_" + UUID.randomUUID().toString());
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File normalizedWav = new File(tempDir, "normalized_16k.wav");
        File outputVttFile = new File(tempDir, "transcript.vtt");

        try {
            // Bước 1: Chuẩn hóa Audio sang chuẩn WAV 16kHz, Mono bằng FFmpeg (chạy mất ~1-2 giây)
            log.info("✂️ Đang chuẩn hóa âm thanh sang WAV 16kHz, Mono...");
            ProcessBuilder pbConvert = new ProcessBuilder(
                    ffmpegPath, "-y",
                    "-nostdin",
                    "-loglevel", "warning",
                    "-i", audioFile.getAbsolutePath(),
                    "-ar", "16000",
                    "-ac", "1",
                    "-c:a", "pcm_s16le",
                    normalizedWav.getAbsolutePath()
            );
            pbConvert.inheritIO();
            Process procConvert = pbConvert.start();
            if (!procConvert.waitFor(2, TimeUnit.MINUTES) || procConvert.exitValue() != 0) {
                throw new RuntimeException("Lỗi chuẩn hóa âm thanh WAV 16kHz.");
            }

            // Bước 2: Chạy Whisper.cpp bóc băng trọn gói ra tệp WebVTT
            // Tự động phân giải đường dẫn tương đối khi chạy từ thư mục gốc cha hoặc thư mục con worker-service
            File whisperExecutable = new File(whisperPath);
            if (!whisperExecutable.exists()) {
                whisperExecutable = new File("worker-service/" + whisperPath);
            }
            if (!whisperExecutable.exists()) {
                throw new RuntimeException("Không tìm thấy file thực thi Whisper tại: " + whisperPath 
                        + " hoặc worker-service/" + whisperPath + ". Vui lòng kiểm tra lại vị trí đặt file!");
            }

            File model = new File(modelPath);
            if (!model.exists()) {
                model = new File("worker-service/" + modelPath);
            }
            if (!model.exists()) {
                throw new RuntimeException("Không tìm thấy file Model Whisper tại: " + modelPath 
                        + " hoặc worker-service/" + modelPath + ". Vui lòng kiểm tra lại vị trí đặt file!");
            }

            log.info("🚀 Đang chạy xử lý nơ-ron cục bộ bằng Whisper.cpp (Model: {})...", model.getAbsolutePath());
            ProcessBuilder pbWhisper = new ProcessBuilder(
                    whisperExecutable.getAbsolutePath(),
                    "-m", model.getAbsolutePath(),
                    "-f", normalizedWav.getAbsolutePath(),
                    "-l", language,
                    "-ovtt", // Tự động xuất đầu ra chuẩn WebVTT
                    "-of", outputVttFile.getAbsolutePath().replace(".vtt", "") // Tên file đầu ra (bỏ đuôi vtt vì tool tự thêm)
            );
            
            pbWhisper.inheritIO();
            Process procWhisper = pbWhisper.start();
            
            // Cho phép tối đa 15 phút xử lý cục bộ (thường chỉ tốn bằng 1/10 thời lượng video)
            boolean completed = procWhisper.waitFor(15, TimeUnit.MINUTES);
            if (!completed) {
                procWhisper.destroyForcibly();
                throw new RuntimeException("Quá thời gian bóc băng (Timeout 15 phút).");
            }
            if (procWhisper.exitValue() != 0) {
                throw new RuntimeException("Whisper.cpp kết thúc với mã lỗi: " + procWhisper.exitValue());
            }

            // Bước 3: Đọc tệp tin WebVTT sinh ra và trả về nội dung chữ
            if (!outputVttFile.exists()) {
                throw new RuntimeException("Không tìm thấy file WebVTT đầu ra sau khi Whisper kết thúc.");
            }

            String vttContent = Files.readString(outputVttFile.toPath());
            log.info("✅ Bóc băng Offline thành công! Độ dài VTT: {} ký tự", vttContent.length());
            return vttContent;

        } catch (Exception e) {
            log.error("❌ Tiến trình bóc băng Whisper.cpp thất bại: {}", e.getMessage(), e);
            throw new RuntimeException("Whisper Local STT failed: " + e.getMessage(), e);
        } finally {
            // Dọn dẹp các tệp wav, vtt tạm thời cục bộ để tránh đầy ổ cứng
            org.springframework.util.FileSystemUtils.deleteRecursively(tempDir);
            log.info("🧹 Đã dọn dẹp thư mục tạm của Whisper: {}", tempDir.getAbsolutePath());
        }
    }
}
