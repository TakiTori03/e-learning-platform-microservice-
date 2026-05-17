package com.hust.mediaservice.strategy;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageStrategy {

    String uploadFile(MultipartFile file, String folderName) throws IOException;
    void uploadDirectory(String localPath, String remotePath) throws IOException;
    void deleteFile(String fileUrl) throws IOException;
    byte[] getFile(String path) throws IOException;
    String getProviderName();
    
    void downloadFileToLocal(String path, java.io.File destinationFile) throws IOException;
    String generatePresignedUploadUrl(String objectKey, int expirationMinutes);
}
