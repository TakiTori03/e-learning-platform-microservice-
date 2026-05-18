package com.hust.workerservice.strategy.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.*;
import com.hust.workerservice.strategy.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Date;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;

@Component("S3")
@RequiredArgsConstructor
@Slf4j
public class MinioStorageStrategy implements StorageStrategy {

    private final AmazonS3 s3Client;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    @Value("${app.minio.endpoint}")
    private String endpoint;

    @Override
    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        ensureBucketExists();

        String prefix = (folderName == null || folderName.isBlank()) ? "" : folderName + "/";
        String fileName = prefix + UUID.randomUUID() + "_" + file.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        s3Client.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead));

        return String.format("%s/%s/%s", endpoint, bucketName, fileName);
    }

    private void ensureBucketExists() {
        if (!s3Client.doesBucketExistV2(bucketName)) {
            s3Client.createBucket(bucketName);
            
            // Public anonymous read policy
            String policy = "{\n" +
                    "    \"Version\": \"2012-10-17\",\n" +
                    "    \"Statement\": [\n" +
                    "        {\n" +
                    "            \"Sid\": \"PublicRead\",\n" +
                    "            \"Effect\": \"Allow\",\n" +
                    "            \"Principal\": \"*\",\n" +
                    "            \"Action\": [\"s3:GetObject\"],\n" +
                    "            \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";
            s3Client.setBucketPolicy(bucketName, policy);
            log.info("Created bucket {} with public read policy", bucketName);
        }
    }

    @Override
    public void uploadDirectory(String localPath, String remotePath) throws IOException {
        ensureBucketExists();

        Path sourcePath = Paths.get(localPath);
        try (Stream<Path> paths = Files.walk(sourcePath)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                String key = remotePath + "/" + sourcePath.relativize(file).toString().replace("\\", "/");
                s3Client.putObject(new PutObjectRequest(bucketName, key, file.toFile())
                        .withCannedAcl(CannedAccessControlList.PublicRead));
                log.info("Uploaded {} to S3", key);
            });
        }
    }

    @Override
    public void deleteFile(String fileUrl) throws IOException {
        String key;
        String prefix = endpoint + "/" + bucketName + "/";
        if (fileUrl.startsWith(prefix)) {
            key = fileUrl.substring(prefix.length());
        } else {
            key = fileUrl;
        }
        s3Client.deleteObject(bucketName, key);
    }

    @Override
    public byte[] getFile(String path) throws IOException {
        try (S3Object s3Object = s3Client.getObject(bucketName, path)) {
            return s3Object.getObjectContent().readAllBytes();
        } catch (Exception e) {
            throw new IOException("Failed to fetch file from S3: " + path, e);
        }
    }

    @Override
    public void downloadFileToLocal(String path, java.io.File destinationFile) throws IOException {
        log.info("Streaming direct file download from MinIO to local path: {}", destinationFile.getAbsolutePath());
        String key;
        String prefix = endpoint + "/" + bucketName + "/";
        if (path != null && path.startsWith(prefix)) {
            key = path.substring(prefix.length());
        } else {
            key = path;
        }
        try (S3Object s3Object = s3Client.getObject(bucketName, key);
             java.io.InputStream in = s3Object.getObjectContent()) {
            
            java.nio.file.Files.copy(in, destinationFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Completed direct download of {} from MinIO.", key);
        } catch (Exception e) {
            throw new IOException("Streaming download failed for S3 path: " + key, e);
        }
    }

    @Override
    public String getProviderName() {
        return "S3";
    }

    @Override
    public String generatePresignedUploadUrl(String objectKey, int expirationMinutes) {
        ensureBucketExists();
        
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000L * 60 * expirationMinutes;
        expiration.setTime(expTimeMillis);

        log.info("Generating Presigned PUT URL for object: {} valid for {} minutes", objectKey, expirationMinutes);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(HttpMethod.PUT)
                        .withExpiration(expiration);

        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }
}
