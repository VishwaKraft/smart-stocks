package com.smartstocks.product.service.impl;

import com.smartstocks.product.service.ITemplateImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
public class TemplateImageServiceImpl implements ITemplateImageService {

    private static final Logger log = LoggerFactory.getLogger(TemplateImageServiceImpl.class);

    private static final String FOLDER = "template-images";

    private S3Client s3Client;

    @Value("${aws.s3.accessKey}")
    private String awsAccessKey;

    @Value("${aws.s3.secretKey}")
    private String awsSecretKey;

    @Value("${aws.s3.region}")
    private String awsRegion;

    @Value("${aws.s3.bucket}")
    private String awsBucketName;

    @Value("${aws.s3.endpoint}")
    private String awsEndpoint;

    @PostConstruct
    public void init() {
        s3Client = S3Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .endpointOverride(URI.create(awsEndpoint))
                .region(Region.of(awsRegion))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(awsAccessKey, awsSecretKey)))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build())
                .build();
    }

    @Override
    public String uploadImage(MultipartFile file) {
        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image (jpg, png, gif, webp, etc.)");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded image file: " + e.getMessage(), e);
        }

        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "image";
        String s3Key = FOLDER + "/" + UUID.randomUUID() + "-" + originalFilename;

        log.info("Uploading template image to S3 with key: {}", s3Key);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(awsBucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(fileBytes));
        } catch (AwsServiceException e) {
            log.error("S3 upload failed for template image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image to S3: " + e.getMessage(), e);
        }

        // Build the Supabase public URL.
        // S3 endpoint format:  https://<project>.storage.supabase.co/storage/v1/s3
        // Public URL format:   https://<project>.storage.supabase.co/storage/v1/object/public/<bucket>/<key>
        String publicBase = awsEndpoint.replace("/s3", "/object/public");
        String publicUrl = publicBase + "/" + awsBucketName + "/" + s3Key;

        log.info("Template image uploaded successfully. Public URL: {}", publicUrl);
        return publicUrl;
    }
}
