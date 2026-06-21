package com.smartstocks.product.repository;

import com.smartstocks.product.models.SegmentUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SegmentUserRepository extends JpaRepository<SegmentUser, Long> {
    List<SegmentUser> findBySegmentId(Long segmentId);
}
