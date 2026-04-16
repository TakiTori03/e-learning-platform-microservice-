package com.hust.mediaservice.strategy.impl;

import com.hust.mediaservice.strategy.StorageStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component("LOCAL")
public class LocalStorageStrategy implements StorageStrategy {

    @Value("${app.storage.local.path:./uploads}")
    private String uploadDir;

    @Value("${app.storage.local.base-url:http://localhost:8080/files/}")
    private String baseUrl;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        Path root = Paths.get(uploadDir);
        if (!Files.exists(root)) Files.createDirectories(root);

        String extension = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        
        String fileName = UUID.randomUUID().toString() + extension;
        Files.copy(file.getInputStream(), root.resolve(fileName));
        return baseUrl + fileName;
    }

    @Override
    public void deleteFile(String fileUrl) throws IOException {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        Files.deleteIfExists(Paths.get(uploadDir).resolve(fileName));
    }

    @Override
    public String getProviderName() {
        return "LOCAL";
    }
}
