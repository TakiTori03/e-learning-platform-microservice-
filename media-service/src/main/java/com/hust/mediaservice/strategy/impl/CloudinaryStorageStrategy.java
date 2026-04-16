package com.hust.mediaservice.strategy.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hust.mediaservice.strategy.StorageStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Component("CLOUDINARY")
@RequiredArgsConstructor
public class CloudinaryStorageStrategy implements StorageStrategy {

    private final Cloudinary cloudinary;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
        return result.get("secure_url").toString();
    }

    @Override
    public void deleteFile(String fileUrl) throws IOException {
        // Implementation for deletion if public_id is known
    }

    @Override
    public String getProviderName() {
        return "CLOUDINARY";
    }
}
