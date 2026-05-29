package com.smartstocks.product.repository;

import com.smartstocks.product.models.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    boolean existsByShortId(String shortId);

    Optional<ShortLink> findByShortId(String shortId);

    @Modifying
    @Transactional
    @Query("UPDATE ShortLink l SET l.clickCount = l.clickCount + 1 WHERE l.shortId = :shortId")
    int incrementClickCountByShortId(@Param("shortId") String shortId);
}
