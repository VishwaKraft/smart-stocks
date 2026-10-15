package com.smartstocks.product.service.impl;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.smartstocks.product.models.Segment;
import com.smartstocks.product.models.SegmentUser;
import com.smartstocks.product.repository.SegmentRepository;
import com.smartstocks.product.repository.SegmentUserRepository;
import com.smartstocks.product.service.CampaignEventLogger;
import com.smartstocks.product.service.ISegmentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class SegmentServiceImpl implements ISegmentService {

    private static final Logger log = LoggerFactory.getLogger(SegmentServiceImpl.class);
    private static final int BATCH_SIZE = 2000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Executor executor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "segment-csv-worker-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            }
    );

    private static class SegmentUserRecord {
        final String userId;
        final String emailId;
        final String phoneNumber;
        final String dataJson;

        SegmentUserRecord(String userId, String emailId, String phoneNumber, String dataJson) {
            this.userId = userId;
            this.emailId = emailId;
            this.phoneNumber = phoneNumber;
            this.dataJson = dataJson;
        }
    }

    private final SegmentRepository segmentRepository;
    private final SegmentUserRepository segmentUserRepository;
    private final JdbcTemplate jdbcTemplate;
    private final CampaignEventLogger eventLogger;

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

    @Transactional
    public void insertBatch(Long segmentId, List<SegmentUserRecord> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        final List<SegmentUserRecord> records = new ArrayList<>(batch);
        String sql = "INSERT INTO segment_users (segment_id, user_id, email_id, phone_number, data) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SegmentUserRecord r = records.get(i);
                ps.setLong(1, segmentId);
                ps.setString(2, r.userId);
                ps.setString(3, r.emailId);
                ps.setString(4, r.phoneNumber);
                ps.setString(5, r.dataJson);
            }

            @Override
            public int getBatchSize() {
                return records.size();
            }
        });
    }

    @Override
    public Segment uploadCsvSegment(String name, String description, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        // Read file bytes ONCE so we can use them for both S3 upload and background CSV parsing.
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded CSV file: " + e.getMessage(), e);
        }

        // Fast upfront validation of the CSV header (< 1ms) so caller gets immediate error if invalid
        validateCsvHeader(fileBytes);

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "segment.csv";
        String s3Key = "segments/" + UUID.randomUUID() + "-" + filename;

        log.info("Uploading CSV to S3 with key: {}", s3Key);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(awsBucketName)
                    .key(s3Key)
                    .contentType(file.getContentType() != null ? file.getContentType() : "text/csv")
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(fileBytes));
        } catch (AwsServiceException e) {
            log.error("AmazonServiceException: Error Message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload CSV to S3. Error: " + e.getMessage(), e);
        }

        // 1. Create and save entry in segments table immediately
        Segment segment = new Segment();
        segment.setName(name);
        segment.setDescription(description);
        segment.setSegmentType("CSV");
        segment.setStatus("PROCESSING");
        segment.setS3Path("s3://" + awsBucketName + "/" + s3Key);
        segment = segmentRepository.save(segment);

        Map<String, Object> eventInfo = new java.util.HashMap<>();
        eventInfo.put("segmentId", segment.getId());
        eventInfo.put("segmentName", segment.getName());
        eventInfo.put("s3Path", segment.getS3Path());
        eventLogger.log("SEGMENT_CREATED", eventInfo);

        // 2. Offload the heavy lifting (parsing CSV and saving users to segment_users table) to a different thread
        final Long segmentId = segment.getId();
        final String segmentName = segment.getName();
        final byte[] csvBytes = fileBytes;

        executor.execute(() -> {
            processCsvInBackground(segmentId, segmentName, csvBytes);
        });

        // 3. Return the segment entry immediately
        return segment;
    }

    private void validateCsvHeader(byte[] fileBytes) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8))) {
            String headerLine = null;
            while ((headerLine = reader.readLine()) != null) {
                if (headerLine.startsWith("\uFEFF")) {
                    headerLine = headerLine.substring(1);
                }
                if (!headerLine.trim().isEmpty()) {
                    break;
                }
            }

            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            char delimiter = detectDelimiter(headerLine);
            List<String> rawHeaders = parseCsvLine(headerLine, delimiter);
            boolean hasEmail = false;
            for (String rawHeader : rawHeaders) {
                if (isEmailHeader(normalizeHeader(rawHeader))) {
                    hasEmail = true;
                    break;
                }
            }

            if (!hasEmail) {
                throw new IllegalArgumentException("CSV must contain an 'emailid' column (found columns: " + String.join(", ", rawHeaders) + ")");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV header: " + e.getMessage(), e);
        }
    }

    private void processCsvInBackground(Long segmentId, String segmentName, byte[] csvBytes) {
        log.info("Background worker [{}] started processing CSV for segment id: {}, name: [{}]",
                Thread.currentThread().getName(), segmentId, segmentName);
        long startTime = System.currentTimeMillis();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8), 32768)) {
            long total = parseCsvAndSaveUsers(segmentId, segmentName, reader);
            segmentRepository.findById(segmentId).ifPresent(s -> {
                s.setStatus("READY");
                segmentRepository.save(s);
            });
            long duration = System.currentTimeMillis() - startTime;
            log.info("Background worker [{}] completed processing segment id: {}. Total users saved: {} in {} ms",
                    Thread.currentThread().getName(), segmentId, total, duration);
        } catch (Exception e) {
            log.error("Background CSV processing failed for segment id {}: {}", segmentId, e.getMessage(), e);
            segmentRepository.findById(segmentId).ifPresent(s -> {
                s.setStatus("FAILED");
                segmentRepository.save(s);
            });
        }
    }

    @Override
    @Transactional
    public Segment createSqlSegment(String name, String description, String sqlQuery) {
        Segment segment = new Segment();
        segment.setName(name);
        segment.setDescription(description);
        segment.setSegmentType("SQL");
        segment.setStatus("READY");
        segment.setSqlQuery(sqlQuery);

        segment = segmentRepository.save(segment);

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sqlQuery);
            List<SegmentUserRecord> batch = new ArrayList<>(BATCH_SIZE);
            long count = 0;

            for (Map<String, Object> row : rows) {
                String emailId = null;
                String userId = null;
                String phoneNumber = null;
                Map<String, Object> extraData = new HashMap<>();

                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String col = normalizeHeader(entry.getKey());
                    if (isEmailHeader(col)) {
                        emailId = String.valueOf(entry.getValue());
                    } else if (isUserIdHeader(col)) {
                        userId = String.valueOf(entry.getValue());
                    } else if (isPhoneHeader(col)) {
                        phoneNumber = String.valueOf(entry.getValue());
                    } else {
                        extraData.put(entry.getKey(), entry.getValue());
                    }
                }

                if (emailId != null && !emailId.trim().isEmpty() && !emailId.equalsIgnoreCase("null")) {
                    emailId = emailId.trim();
                    extraData.put("email", emailId);
                    if (userId != null && !userId.trim().isEmpty() && !userId.equalsIgnoreCase("null")) {
                        userId = userId.trim();
                        extraData.put("userId", userId);
                    } else {
                        userId = null;
                    }
                    if (phoneNumber != null && !phoneNumber.trim().isEmpty() && !phoneNumber.equalsIgnoreCase("null")) {
                        phoneNumber = phoneNumber.trim();
                        extraData.put("phoneNumber", phoneNumber);
                    } else {
                        phoneNumber = null;
                    }

                    String dataJson = null;
                    try {
                        dataJson = OBJECT_MAPPER.writeValueAsString(extraData);
                    } catch (Exception ignored) {}

                    batch.add(new SegmentUserRecord(userId, emailId, phoneNumber, dataJson));
                    count++;

                    if (batch.size() >= BATCH_SIZE) {
                        insertBatch(segment.getId(), batch);
                        batch.clear();
                    }
                }
            }

            if (!batch.isEmpty()) {
                insertBatch(segment.getId(), batch);
                batch.clear();
            }
            segment.setUserCount(count);
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
    public Segment createS3PathSegment(String name, String description, String s3Path) {
        if (!s3Path.startsWith("s3://")) {
            throw new IllegalArgumentException("S3 path must start with s3://");
        }
        
        String withoutPrefix = s3Path.substring(5);
        int slashIdx = withoutPrefix.indexOf('/');
        if (slashIdx == -1) {
            throw new IllegalArgumentException("Invalid S3 path format.");
        }
        String bucket = withoutPrefix.substring(0, slashIdx);
        String key = withoutPrefix.substring(slashIdx + 1);

        log.info("Creating S3 segment for bucket: {}, key: {}", bucket, key);

        Segment segment = new Segment();
        segment.setName(name);
        segment.setDescription(description);
        segment.setSegmentType("S3");
        segment.setStatus("PROCESSING");
        segment.setS3Path(s3Path);
        segment = segmentRepository.save(segment);

        Map<String, Object> eventInfo = new java.util.HashMap<>();
        eventInfo.put("segmentId", segment.getId());
        eventInfo.put("segmentName", segment.getName());
        eventInfo.put("s3Path", segment.getS3Path());
        eventLogger.log("SEGMENT_CREATED", eventInfo);

        final Long segmentId = segment.getId();
        final String segmentName = segment.getName();

        executor.execute(() -> {
            processS3CsvInBackground(segmentId, segmentName, bucket, key);
        });

        return segment;
    }

    private void processS3CsvInBackground(Long segmentId, String segmentName, String bucket, String key) {
        log.info("Background worker [{}] downloading and processing S3 CSV for segment id: {}, bucket: {}, key: {}",
                Thread.currentThread().getName(), segmentId, bucket, key);
        long startTime = System.currentTimeMillis();
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s3Object, StandardCharsets.UTF_8), 32768)) {
                long total = parseCsvAndSaveUsers(segmentId, segmentName, reader);
                segmentRepository.findById(segmentId).ifPresent(s -> {
                    s.setStatus("READY");
                    segmentRepository.save(s);
                });
                long duration = System.currentTimeMillis() - startTime;
                log.info("Background worker [{}] completed processing S3 segment id: {}. Total users: {} in {} ms",
                        Thread.currentThread().getName(), segmentId, total, duration);
            }
        } catch (Exception e) {
            log.error("Background S3 CSV processing failed for segment id {}: {}", segmentId, e.getMessage(), e);
            segmentRepository.findById(segmentId).ifPresent(s -> {
                s.setStatus("FAILED");
                segmentRepository.save(s);
            });
        }
    }

    private long parseCsvAndSaveUsers(Long segmentId, String segmentName, BufferedReader reader) throws IOException {
        String headerLine = null;
        while ((headerLine = reader.readLine()) != null) {
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }
            if (!headerLine.trim().isEmpty()) {
                break;
            }
        }

        if (headerLine == null) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        char delimiter = detectDelimiter(headerLine);
        List<String> rawHeaders = parseCsvLine(headerLine, delimiter);
        int emailIdx = -1, userIdx = -1, phoneIdx = -1;
        for (int i = 0; i < rawHeaders.size(); i++) {
            String norm = normalizeHeader(rawHeaders.get(i));
            if (isEmailHeader(norm) && emailIdx == -1) {
                emailIdx = i;
            } else if (isUserIdHeader(norm) && userIdx == -1) {
                userIdx = i;
            } else if (isPhoneHeader(norm) && phoneIdx == -1) {
                phoneIdx = i;
            }
        }

        if (emailIdx == -1) {
            throw new IllegalArgumentException("CSV must contain an 'emailid' column (found columns: " + String.join(", ", rawHeaders) + ")");
        }

        final int headerSize = rawHeaders.size();
        final String[] cleanedHeaderKeys = new String[headerSize];
        final List<Integer> extraColIndices = new ArrayList<>();

        for (int i = 0; i < headerSize; i++) {
            cleanedHeaderKeys[i] = cleanToken(rawHeaders.get(i));
            if (i != emailIdx && i != userIdx && i != phoneIdx && !cleanedHeaderKeys[i].isEmpty()) {
                extraColIndices.add(i);
            }
        }

        List<SegmentUserRecord> batch = new ArrayList<>(BATCH_SIZE);
        long totalInserted = 0;
        long startTime = System.currentTimeMillis();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> parts = parseCsvLine(line, delimiter);
            if (parts.size() <= emailIdx) {
                continue;
            }

            String email = cleanToken(parts.get(emailIdx));
            if (email.isEmpty()) {
                continue;
            }

            String uid = null;
            if (userIdx != -1 && parts.size() > userIdx) {
                String token = cleanToken(parts.get(userIdx));
                if (!token.isEmpty()) uid = token;
            }

            String phone = null;
            if (phoneIdx != -1 && parts.size() > phoneIdx) {
                String token = cleanToken(parts.get(phoneIdx));
                if (!token.isEmpty()) phone = token;
            }

            Map<String, Object> extraData = new HashMap<>(extraColIndices.size() + 3);
            for (int idx : extraColIndices) {
                String val = parts.size() > idx ? cleanToken(parts.get(idx)) : "";
                extraData.put(cleanedHeaderKeys[idx], val);
            }
            extraData.put("email", email);
            if (uid != null) extraData.put("userId", uid);
            if (phone != null) extraData.put("phoneNumber", phone);

            String dataJson = null;
            try {
                dataJson = OBJECT_MAPPER.writeValueAsString(extraData);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize extra data for email {}: {}", email, e.getMessage());
            }

            batch.add(new SegmentUserRecord(uid, email, phone, dataJson));

            if (batch.size() >= BATCH_SIZE) {
                insertBatch(segmentId, batch);
                totalInserted += batch.size();
                batch.clear();
                if (totalInserted % 20000 == 0) {
                    log.info("Loaded {} segment users so far for segment [{}]...", totalInserted, segmentName);
                }
            }
        }

        if (!batch.isEmpty()) {
            insertBatch(segmentId, batch);
            totalInserted += batch.size();
            batch.clear();
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Successfully loaded and inserted {} segment users for segment [{}] in {} ms",
                totalInserted, segmentName, duration);

        return totalInserted;
    }

    private char detectDelimiter(String line) {
        int commas = 0, semicolons = 0, tabs = 0;
        for (char c : line.toCharArray()) {
            if (c == ',') commas++;
            else if (c == ';') semicolons++;
            else if (c == '\t') tabs++;
        }
        if (semicolons > commas && semicolons >= tabs) return ';';
        if (tabs > commas && tabs > semicolons) return '\t';
        return ',';
    }

    private List<String> parseCsvLine(String line, char delimiter) {
        if (line == null || line.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();

        // Fast path: if line has no quotes, split directly without quote state-machine allocations
        if (line.indexOf('"') == -1 && line.indexOf('\'') == -1) {
            int len = line.length();
            int start = 0;
            for (int i = 0; i < len; i++) {
                if (line.charAt(i) == delimiter) {
                    tokens.add(cleanToken(line.substring(start, i)));
                    start = i + 1;
                }
            }
            tokens.add(cleanToken(line.substring(start, len)));
            return tokens;
        }

        // Standard quoted CSV parsing
        StringBuilder sb = new StringBuilder(line.length());
        boolean inQuotes = false;
        int len = line.length();
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < len && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(cleanToken(sb.toString()));
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(cleanToken(sb.toString()));
        return tokens;
    }

    private String cleanToken(String token) {
        if (token == null) return "";
        String s = token.trim();
        if (s.isEmpty()) return "";
        if (s.indexOf('\uFEFF') != -1 || s.indexOf('\u200B') != -1) {
            s = s.replace("\uFEFF", "").replace("\u200B", "").trim();
        }
        if ((s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) ||
            (s.startsWith("'") && s.endsWith("'") && s.length() >= 2)) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private String normalizeHeader(String header) {
        if (header == null) return "";
        String s = cleanToken(header).toLowerCase();
        return s.replaceAll("[^a-z0-9]", "");
    }

    private boolean isEmailHeader(String normalized) {
        return normalized.equals("emailid")
                || normalized.equals("email")
                || normalized.equals("emailaddress")
                || normalized.equals("mail");
    }

    private boolean isUserIdHeader(String normalized) {
        return normalized.equals("userid")
                || normalized.equals("id")
                || normalized.equals("uid");
    }

    private boolean isPhoneHeader(String normalized) {
        return normalized.equals("phonenumber")
                || normalized.equals("phone")
                || normalized.equals("mobilenumber")
                || normalized.equals("mobile")
                || normalized.equals("contactnumber")
                || normalized.equals("contact");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Segment> getAllSegments() {
        List<Segment> segments = segmentRepository.findAll();
        segments.forEach(segment -> segment.setUserCount(segmentUserRepository.countBySegmentId(segment.getId())));
        return segments;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Segment> getSegmentById(Long id) {
        return segmentRepository.findById(id).map(segment -> {
            segment.setUserCount(segmentUserRepository.countBySegmentId(id));
            return segment;
        });
    }

    @Override
    @Transactional
    public void deleteSegment(Long id) {
        jdbcTemplate.update("DELETE FROM segment_users WHERE segment_id = ?", id);
        segmentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SegmentUser> getSegmentUsers(Long segmentId) {
        return segmentUserRepository.findBySegmentId(segmentId);
    }

    // Visible for testing
    void setS3Client(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    void setAwsBucketName(String awsBucketName) {
        this.awsBucketName = awsBucketName;
    }

    void setExecutor(Executor executor) {
        this.executor = executor;
    }

    Executor getExecutor() {
        return this.executor;
    }
}
