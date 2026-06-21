package com.smartstocks.product.controllers;

import com.smartstocks.product.models.Segment;
import com.smartstocks.product.models.SegmentUser;
import com.smartstocks.product.service.ISegmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/segments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SegmentController {

    private final ISegmentService segmentService;

    @PostMapping("/csv")
    public ResponseEntity<?> uploadCsvSegment(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("file") MultipartFile file) {
        try {
            Segment segment = segmentService.uploadCsvSegment(name, description, file);
            return ResponseEntity.ok(segment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create CSV segment: " + e.getMessage());
        }
    }

    @PostMapping("/sql")
    public ResponseEntity<?> createSqlSegment(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            String sqlQuery = request.get("sqlQuery");
            if (name == null || sqlQuery == null) {
                return ResponseEntity.badRequest().body("Name and SQL query are required");
            }
            Segment segment = segmentService.createSqlSegment(name, description, sqlQuery);
            return ResponseEntity.ok(segment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create SQL segment: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Segment>> getAllSegments() {
        return ResponseEntity.ok(segmentService.getAllSegments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Segment> getSegment(@PathVariable Long id) {
        return segmentService.getSegmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<List<SegmentUser>> getSegmentUsers(@PathVariable Long id) {
        return ResponseEntity.ok(segmentService.getSegmentUsers(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSegment(@PathVariable Long id) {
        try {
            segmentService.deleteSegment(id);
            return ResponseEntity.ok("Segment deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete segment: " + e.getMessage());
        }
    }
}
