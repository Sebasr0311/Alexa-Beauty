package com.alexabeauty.catalog.repository;

import com.alexabeauty.catalog.domain.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {
    Optional<Brand> findBySlug(String slug);
    List<Brand> findByActiveTrue();
    boolean existsBySlug(String slug);
}
