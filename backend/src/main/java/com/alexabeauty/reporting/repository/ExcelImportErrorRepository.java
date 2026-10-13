package com.alexabeauty.reporting.repository;

import com.alexabeauty.reporting.domain.ExcelImportError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExcelImportErrorRepository extends JpaRepository<ExcelImportError, UUID> {
    List<ExcelImportError> findByJobId(UUID jobId);
    List<ExcelImportError> findByJobIdOrderByRowNumberAsc(UUID jobId);
}
