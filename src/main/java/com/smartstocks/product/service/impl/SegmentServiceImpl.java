package com.smartstocks.product.service.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.smartstocks.product.models.Segment;
import com.smartstocks.product.models.SegmentUser;
import com.smartstocks.product.repository.SegmentRepository;
import com.smartstocks.product.repository.SegmentUserRepository;
import com.smartstocks.product.service.CampaignEventLogger;
import com.smartstocks.product.service.ISegmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SegmentServiceImpl implements ISegmentService {

    private final SegmentRepository segmentRepository;
    private final SegmentUserRepository segmentUserRepository;
    private final JdbcTemplate jdbcTemplate;
    private final CampaignEventLogger eventLogger;

    private AmazonS3 s3Client;

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
        BasicAWSCredentials creds = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsEndpoint, awsRegion))
                .withPathStyleAccessEnabled(true)
                .build();
    }

    @Override
    @Transactional
    public Segment uploadCsvSegment(String name, String description, MultipartFile file) {
        String s3Key = "segments/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            s3Client.putObject(awsBucketName, s3Key, file.getInputStream(), metadata);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload CSV to S3: " + e.getMessage(), e);
        }

        Segment segment = new Segment();
        segment.setName(name);
        segment.setDescription(description);
        segment.setSegmentType("CSV");
        segment.setS3Path("s3://" + awsBucketName + "/" + s3Key);
        segment = segmentRepository.save(segment);

        Map<String, Object> eventInfo = new java.util.HashMap<>();
        eventInfo.put("segmentId", segment.getId());
        eventInfo.put("segmentName", segment.getName());
        eventInfo.put("s3Path", segment.getS3Path());
        eventLogger.log("SEGMENT_CREATED", eventInfo);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine != null) {
                String[] headers = headerLine.split(",");
                int emailIdx = -1, userIdx = -1, phoneIdx = -1;
                for (int i = 0; i < headers.length; i++) {
                    String h = headers[i].trim().toLowerCase();
                    if (h.equals("emailid") || h.equals("email")) emailIdx = i;
                    if (h.equals("userid") || h.equals("id")) userIdx = i;
                    if (h.equals("phone_number") || h.equals("phonenumber") || h.equals("phone")) phoneIdx = i;
                }

                if (emailIdx == -1) {
                    throw new IllegalArgumentException("CSV must contain an 'emailid' column");
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length > emailIdx) {
                        String email = parts[emailIdx].trim();
                        if (!email.isEmpty()) {
                            SegmentUser su = new SegmentUser();
                            su.setSegment(segment);
                            su.setEmailId(email);
                            if (userIdx != -1 && parts.length > userIdx) su.setUserId(parts[userIdx].trim());
                            if (phoneIdx != -1 && parts.length > phoneIdx) su.setPhoneNumber(parts[phoneIdx].trim());
                            segmentUserRepository.save(su);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV", e);
        }
        
        return segment;
    }

    @Override
    @Transactional
    public Segment createSqlSegment(String name, String description, String sqlQuery) {
        Segment segment = new Segment();
        segment.setName(name);
        segment.setDescription(description);
        segment.setSegmentType("SQL");
        segment.setSqlQuery(sqlQuery);
        
        segment = segmentRepository.save(segment);

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sqlQuery);
            for (Map<String, Object> row : rows) {
                SegmentUser su = new SegmentUser();
                su.setSegment(segment);
                
                String emailId = null;
                String userId = null;
                String phoneNumber = null;

                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String col = entry.getKey().toLowerCase();
                    if (col.equals("emailid") || col.equals("email")) emailId = String.valueOf(entry.getValue());
                    if (col.equals("userid") || col.equals("id")) userId = String.valueOf(entry.getValue());
                    if (col.equals("phone_number") || col.equals("phonenumber") || col.equals("phone")) phoneNumber = String.valueOf(entry.getValue());
                }

                if (emailId != null && !emailId.trim().isEmpty() && !emailId.equals("null")) {
                    su.setEmailId(emailId);
                    su.setUserId(userId);
                    su.setPhoneNumber(phoneNumber);
                    segmentUserRepository.save(su);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute SQL query: " + e.getMessage(), e);
        }

        Map<String, Object> eventInfo = new java.util.HashMap<>();
        eventInfo.put("segmentId", segment.getId());
        eventInfo.put("segmentName", segment.getName());
        eventInfo.put("type", "SQL");
        eventLogger.log("SEGMENT_CREATED", eventInfo);

        return segment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Segment> getAllSegments() {
        return segmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Segment> getSegmentById(Long id) {
        return segmentRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteSegment(Long id) {
        segmentUserRepository.deleteAll(segmentUserRepository.findBySegmentId(id));
        segmentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SegmentUser> getSegmentUsers(Long segmentId) {
        return segmentUserRepository.findBySegmentId(segmentId);
    }
}
