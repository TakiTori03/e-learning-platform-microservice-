package com.hust.mediaservice.service;

import com.hust.mediaservice.entity.Media;
import com.hust.mediaservice.repository.MediaRepository;
import com.hust.mediaservice.strategy.StorageStrategy;
import com.hust.mediaservice.entity.enums.MediaType;
import com.hust.mediaservice.entity.enums.StorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MediaService {
    private final MediaRepository mediaRepository;
    private final Map<String, StorageStrategy> storageStrategies;

    @Value("${app.storage.provider:LOCAL}")
    private StorageProvider activeProvider;

    public Media upload(MultipartFile file, MediaType type) throws IOException {
        StorageStrategy strategy = storageStrategies.get(activeProvider.name());
        String url = strategy.uploadFile(file);
        
        Media media = Media.builder()
                .fileName(file.getOriginalFilename())
                .fileType(type)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .url(url)
                .provider(activeProvider)
                .ownerId(getUserId())
                .build();

        return mediaRepository.save(media);
    }

    private String getUserId() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
