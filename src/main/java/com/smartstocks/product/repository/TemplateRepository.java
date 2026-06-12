package com.smartstocks.product.repository;

import com.smartstocks.product.models.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    Optional<Template> findByName(String name);

    boolean existsByName(String name);

    List<Template> findAllByIsActiveTrue();

    List<Template> findAllByIsActiveTrueOrderByCreatedAtDesc();
}
