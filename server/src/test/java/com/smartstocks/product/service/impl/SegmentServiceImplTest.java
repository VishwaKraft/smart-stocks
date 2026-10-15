package com.smartstocks.product.service.impl;

import com.smartstocks.product.models.Segment;
import com.smartstocks.product.repository.SegmentRepository;
import com.smartstocks.product.repository.SegmentUserRepository;
import com.smartstocks.product.service.CampaignEventLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SegmentServiceImplTest {

    @Mock
    private SegmentRepository segmentRepository;

    @Mock
    private SegmentUserRepository segmentUserRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CampaignEventLogger eventLogger;

    @Mock
    private S3Client s3Client;

    private SegmentServiceImpl segmentService;

    @BeforeEach
    void setUp() {
        segmentService = new SegmentServiceImpl(segmentRepository, segmentUserRepository, jdbcTemplate, eventLogger);
        segmentService.setS3Client(s3Client);
        segmentService.setAwsBucketName("test-bucket");
        // By default use same-thread execution in unit tests for deterministic assertions
        segmentService.setExecutor(Runnable::run);
    }

    @Test
    void testUploadCsvSegment_ReturnsOkImmediatelyAndSavesInSegmentsTable() throws Exception {
        // Generate CSV with 5,000 rows
        StringBuilder csv = new StringBuilder();
        csv.append("emailid,userId,phone,firstName,tier\n");
        for (int i = 1; i <= 5000; i++) {
            csv.append("user").append(i).append("@test.com,")
               .append("UID_").append(i).append(",")
               .append("+9190000").append(String.format("%05d", i)).append(",")
               .append("User").append(i).append(",")
               .append("Premium\n");
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "large_segment.csv", "text/csv", csv.toString().getBytes(StandardCharsets.UTF_8));

        Segment mockSegment = new Segment();
        mockSegment.setId(42L);
        mockSegment.setName("Large Segment");
        mockSegment.setSegmentType("CSV");
        mockSegment.setStatus("PROCESSING");

        when(segmentRepository.save(any(Segment.class))).thenAnswer(invocation -> {
            Segment s = invocation.getArgument(0);
            s.setId(42L);
            return s;
        });
        when(segmentRepository.findById(42L)).thenReturn(Optional.of(mockSegment));

        Segment result = segmentService.uploadCsvSegment("Large Segment", "5000 users test", file);

        assertNotNull(result);
        assertEquals(42L, result.getId());
        assertEquals("CSV", result.getSegmentType());

        // Verify S3 upload
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        // Verify segment was saved in segments table
        verify(segmentRepository, atLeastOnce()).save(any(Segment.class));

        // Verify batch updates were executed: 5000 rows / batch size 2000 => 3 batch calls (2000, 2000, 1000)
        ArgumentCaptor<BatchPreparedStatementSetter> captor = ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate, times(3)).batchUpdate(
                eq("INSERT INTO segment_users (segment_id, user_id, email_id, phone_number, data) VALUES (?, ?, ?, ?, ?)"),
                captor.capture());

        List<BatchPreparedStatementSetter> capturedSetters = captor.getAllValues();
        assertEquals(3, capturedSetters.size());
        assertEquals(2000, capturedSetters.get(0).getBatchSize());
        assertEquals(2000, capturedSetters.get(1).getBatchSize());
        assertEquals(1000, capturedSetters.get(2).getBatchSize());

        // Verify PreparedStatement bindings for first row
        PreparedStatement ps = mock(PreparedStatement.class);
        capturedSetters.get(0).setValues(ps, 0);
        verify(ps).setLong(1, 42L);
        verify(ps).setString(2, "UID_1");
        verify(ps).setString(3, "user1@test.com");
        verify(ps).setString(4, "+919000000001");

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(ps).setString(eq(5), jsonCaptor.capture());

        String json = jsonCaptor.getValue();
        assertTrue(json.contains("\"firstName\":\"User1\""));
        assertTrue(json.contains("\"tier\":\"Premium\""));
        assertTrue(json.contains("\"email\":\"user1@test.com\""));
    }

    @Test
    void testUploadCsvSegment_OffloadsToDifferentThread() throws Exception {
        // Test that execution actually runs on a separate worker thread
        String csv = "email,userId,phone\nuser1@test.com,U1,111\nuser2@test.com,U2,222\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "async_test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        Segment savedSegment = new Segment();
        savedSegment.setId(500L);
        savedSegment.setName("Async Test");
        savedSegment.setSegmentType("CSV");
        savedSegment.setStatus("PROCESSING");

        when(segmentRepository.save(any(Segment.class))).thenReturn(savedSegment);
        when(segmentRepository.findById(500L)).thenReturn(Optional.of(savedSegment));

        CountDownLatch threadExecuted = new CountDownLatch(1);
        AtomicReference<String> workerThreadName = new AtomicReference<>();

        // Configure a separate thread executor
        segmentService.setExecutor(task -> {
            Thread t = new Thread(() -> {
                workerThreadName.set(Thread.currentThread().getName());
                task.run();
                threadExecuted.countDown();
            }, "custom-csv-worker-thread");
            t.start();
        });

        // Calling uploadCsvSegment returns immediately
        Segment result = segmentService.uploadCsvSegment("Async Test", "Testing async thread offload", file);

        assertNotNull(result);
        assertEquals(500L, result.getId());
        assertEquals("PROCESSING", result.getStatus());

        // Wait for background worker to complete
        assertTrue(threadExecuted.await(5, TimeUnit.SECONDS));
        assertEquals("custom-csv-worker-thread", workerThreadName.get());

        // Verify batch update was executed by the background thread
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
        assertEquals("READY", savedSegment.getStatus());
    }

    @Test
    void testUploadCsvSegment_DifferentDelimiters() {
        String semicolonCsv = "Email;User Id;Phone;City\n" +
                "alice@example.com;U1;111;Mumbai\n" +
                "bob@example.com;U2;222;Delhi\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "semicolon.csv", "text/csv", semicolonCsv.getBytes(StandardCharsets.UTF_8));

        when(segmentRepository.save(any(Segment.class))).thenAnswer(inv -> {
            Segment s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        Segment result = segmentService.uploadCsvSegment("Semicolon Test", "Desc", file);

        assertNotNull(result);
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void testUploadCsvSegment_QuotedValuesWithCommas() throws SQLException {
        String quotedCsv = "email,name,notes\n" +
                "\"john,doe@example.com\",\"Doe, John\",\"Senior Analyst, Market Dept\"\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "quoted.csv", "text/csv", quotedCsv.getBytes(StandardCharsets.UTF_8));

        when(segmentRepository.save(any(Segment.class))).thenAnswer(inv -> {
            Segment s = inv.getArgument(0);
            s.setId(20L);
            return s;
        });

        Segment result = segmentService.uploadCsvSegment("Quoted Test", "Desc", file);

        assertNotNull(result);

        ArgumentCaptor<BatchPreparedStatementSetter> captor = ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture());

        PreparedStatement ps = mock(PreparedStatement.class);
        captor.getValue().setValues(ps, 0);

        verify(ps).setString(3, "john,doe@example.com");

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(ps).setString(eq(5), jsonCaptor.capture());
        assertTrue(jsonCaptor.getValue().contains("\"name\":\"Doe, John\""));
        assertTrue(jsonCaptor.getValue().contains("\"notes\":\"Senior Analyst, Market Dept\""));
    }

    @Test
    void testUploadCsvSegment_MissingEmailColumn_ThrowsExceptionImmediately() {
        String invalidCsv = "name,phone,address\nJohn,123,NY\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "no_email.csv", "text/csv", invalidCsv.getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                segmentService.uploadCsvSegment("Invalid Segment", "Desc", file));

        assertTrue(ex.getMessage().contains("CSV must contain an 'emailid' column"));
        // Verify no segment was saved to database
        verifyNoInteractions(segmentRepository);
    }

    @Test
    void testUploadCsvSegment_EmptyFile_ThrowsExceptionImmediately() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.csv", "text/csv", "".getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                segmentService.uploadCsvSegment("Empty Segment", "Desc", file));

        assertTrue(ex.getMessage().contains("CSV file is empty"));
        verifyNoInteractions(segmentRepository);
    }

    @Test
    void testDeleteSegment_ExecutesDirectJdbcDelete() {
        segmentService.deleteSegment(55L);

        verify(jdbcTemplate, times(1)).update("DELETE FROM segment_users WHERE segment_id = ?", 55L);
        verify(segmentRepository, times(1)).deleteById(55L);
        verifyNoInteractions(segmentUserRepository);
    }

    @Test
    void testCreateSqlSegment_UsesBatchInsert() {
        when(segmentRepository.save(any(Segment.class))).thenAnswer(inv -> {
            Segment s = inv.getArgument(0);
            s.setId(77L);
            return s;
        });

        List<Map<String, Object>> mockRows = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("email", "sql1@test.com");
        row1.put("user_id", "U101");
        row1.put("phone", "+1111");
        row1.put("balance", 5000);
        mockRows.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("mail", "sql2@test.com");
        row2.put("id", "U102");
        row2.put("balance", 8000);
        mockRows.add(row2);

        when(jdbcTemplate.queryForList("SELECT * FROM users WHERE active = true")).thenReturn(mockRows);

        Segment result = segmentService.createSqlSegment("SQL Segment", "Desc", "SELECT * FROM users WHERE active = true");

        assertNotNull(result);
        assertEquals(77L, result.getId());
        assertEquals("SQL", result.getSegmentType());
        assertEquals("READY", result.getStatus());
        assertEquals(2L, result.getUserCount());

        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void testUploadCsvSegment_ScaleBenchmark_100kRows() {
        // Benchmark 1 Lakh (100,000) rows
        int totalRows = 100_000;
        StringBuilder csv = new StringBuilder(totalRows * 50);
        csv.append("email,userId,phone,firstName,city\n");
        for (int i = 1; i <= totalRows; i++) {
            csv.append("user").append(i).append("@domain.com,")
               .append("U").append(i).append(",")
               .append("+9198765").append(String.format("%05d", i % 100000)).append(",")
               .append("Name").append(i).append(",")
               .append("Bangalore\n");
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "100k_emails.csv", "text/csv", csv.toString().getBytes(StandardCharsets.UTF_8));

        when(segmentRepository.save(any(Segment.class))).thenAnswer(inv -> {
            Segment s = inv.getArgument(0);
            s.setId(888L);
            return s;
        });

        long start = System.currentTimeMillis();
        Segment result = segmentService.uploadCsvSegment("100k Benchmark", "Scale test", file);
        long elapsed = System.currentTimeMillis() - start;

        System.out.println(">>> 100,000 rows parsed and batched in: " + elapsed + " ms <<<");

        assertNotNull(result);
        assertEquals(888L, result.getId());
        assertEquals("CSV", result.getSegmentType());

        // 100,000 rows / BATCH_SIZE (2000) = 50 batches
        verify(jdbcTemplate, times(50)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }
}
