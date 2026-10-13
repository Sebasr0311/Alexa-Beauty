package com.alexabeauty.reporting.repository;

import com.alexabeauty.reporting.domain.ExcelImportJob;
import com.alexabeauty.reporting.domain.ImportJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExcelImportJobRepository extends JpaRepository<ExcelImportJob, UUID> {
    Optional<ExcelImportJob> findByFileHash(String fileHash);
    List<ExcelImportJob> findByStatus(ImportJobStatus status);
    boolean existsByFileHashAndStatus(String fileHash, ImportJobStatus status);
    List<ExcelImportJob> findByFileHashOrderByCreatedAtDesc(String fileHash);
    Optional<ExcelImportJob> findTopByFileHashOrderByCreatedAtDesc(String fileHash);
}
