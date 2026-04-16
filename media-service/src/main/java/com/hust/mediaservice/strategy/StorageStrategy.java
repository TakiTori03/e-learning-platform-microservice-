package com.hust.mediaservice.strategy;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageStrategy {
    String uploadFile(MultipartFile file) throws IOException;
    void deleteFile(String fileUrl) throws IOException;
    String getProviderName();
}
