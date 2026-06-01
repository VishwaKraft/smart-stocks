package com.smartstocks.product.repository;

import com.smartstocks.product.models.LinkClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkClickEventRepository extends JpaRepository<LinkClickEvent, Long> {
}
