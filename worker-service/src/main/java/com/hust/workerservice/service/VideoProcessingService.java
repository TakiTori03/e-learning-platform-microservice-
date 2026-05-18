package com.hust.workerservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VideoProcessingService {

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${app.ffprobe.path:ffprobe}")
    private String ffprobePath;

    @Value("${app.storage.local-path:./uploads}")
    private String baseStoragePath;

    public Double getVideoDuration(File inputFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    inputFile.getAbsolutePath()
            );
            Process process = pb.start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    return Double.parseDouble(line.trim());
                }
            }
            process.waitFor();
        } catch (Exception e) {
            log.warn("⚠️ Failed to parse duration using ffprobe: {}. Returning 0.0 as default.", e.getMessage());
        }
        return 0.0;
    }

    public String processToHls(File inputFile, String outputDirName) throws IOException, InterruptedException {
        Path outputDir = Paths.get(baseStoragePath, "hls", outputDirName);
        Files.createDirectories(outputDir);

        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 16); // 16 bytes for AES-128
        File keyFile = new File(outputDir.toFile(), "video.key");
        Files.write(keyFile.toPath(), key.getBytes());

        File keyInfoFile = createKeyInfoFile(outputDir.toFile(), keyFile.getAbsolutePath(), outputDirName);

        String m3u8Output = new File(outputDir.toFile(), "playlist.m3u8").getAbsolutePath();

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-nostdin",
                "-loglevel", "warning",
                "-i", inputFile.getAbsolutePath(),
                "-profile:v", "main",
                "-level", "3.0",
                "-start_number", "0",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-hls_key_info_file", keyInfoFile.getAbsolutePath(),
                "-f", "hls",
                m3u8Output
        );

        pb.inheritIO();
        Process process = pb.start();
        
        // Hard 15-minute timeout gate to prevent infinite loops and zombie processes
        boolean completed = process.waitFor(15, TimeUnit.MINUTES);
        
        if (!completed) {
            log.error("❌ FFmpeg HLS Transcoding HANGED. Exceeded 15-minute limit! Force killing process.");
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg HLS Transcoding failed due to 15-minute time-out.");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
        }

        // Return the relative path to the playlist
        return "hls/" + outputDirName + "/playlist.m3u8";
    }

    public String extractThumbnail(File inputFile, String outputDirName) throws IOException, InterruptedException {
        Path outputDir = Paths.get(baseStoragePath, "hls", outputDirName);
        if (!Files.exists(outputDir)) Files.createDirectories(outputDir);
        
        String thumbnailName = "thumbnail.jpg";
        Path outputPath = outputDir.resolve(thumbnailName);

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-nostdin",
                "-loglevel", "warning",
                "-i", inputFile.getAbsolutePath(),
                "-ss", "00:00:05", // Chụp tại giây thứ 5
                "-vframes", "1",   // Chỉ lấy 1 khung hình
                "-q:v", "2",       // Chất lượng ảnh tốt
                "-update", "1",    // Chỉ định xuất ra một ảnh duy nhất
                outputPath.toAbsolutePath().toString()
        );

        pb.inheritIO();
        Process process = pb.start();
        
        // Thumbnails should be instant (max 1 min limit)
        boolean completed = process.waitFor(1, TimeUnit.MINUTES);
        if (!completed) {
            log.warn("⚠️ FFmpeg thumbnail extraction took too long. Force destroying.");
            process.destroyForcibly();
        }

        return "hls/" + outputDirName + "/" + thumbnailName;
    }

    public String extractAudio(File inputFile, String outputDirName) throws IOException, InterruptedException {
        Path outputDir = Paths.get(baseStoragePath, "hls", outputDirName);
        if (!Files.exists(outputDir)) Files.createDirectories(outputDir);
        
        String audioName = "audio.mp3";
        Path outputPath = outputDir.resolve(audioName);

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-nostdin",
                "-loglevel", "warning",
                "-i", inputFile.getAbsolutePath(),
                "-vn",
                "-acodec", "libmp3lame",
                "-ar", "16000",
                "-ac", "1",
                "-b:a", "32k",
                outputPath.toAbsolutePath().toString()
        );

        pb.inheritIO();
        Process process = pb.start();
        
        boolean completed = process.waitFor(5, TimeUnit.MINUTES);
        if (!completed) {
            log.error("❌ FFmpeg audio extraction timed out.");
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg audio extraction timed out.");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed to extract audio, exit code: " + exitCode);
        }

        return "hls/" + outputDirName + "/" + audioName;
    }

    private File createKeyInfoFile(File dir, String keyFilePath, String outputDirName) throws IOException {
        String keyUri = "/api/v1/media/keys/" + outputDirName;
        File keyInfoFile = new File(dir, "keyinfo.txt");
        String normalizedPath = keyFilePath.replace("\\", "/");
        String content = keyUri + "\n" + normalizedPath;
        Files.write(keyInfoFile.toPath(), content.getBytes());
        return keyInfoFile;
    }
}
