package com.smartstocks.product.service;

import com.smartstocks.product.models.Segment;
import com.smartstocks.product.models.SegmentUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface ISegmentService {
    Segment uploadCsvSegment(String name, String description, MultipartFile file);
    Segment createSqlSegment(String name, String description, String sqlQuery);
    List<Segment> getAllSegments();
    Optional<Segment> getSegmentById(Long id);
    void deleteSegment(Long id);
    List<SegmentUser> getSegmentUsers(Long segmentId);
}
