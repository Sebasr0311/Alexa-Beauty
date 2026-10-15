package com.alexabeauty.reporting.repository;

import com.alexabeauty.reporting.domain.ExcelImportJob;
import com.alexabeauty.reporting.domain.ImportJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExcelImportJobRepository extends JpaRepository<ExcelImportJob, UUID> {
    Optional<ExcelImportJob> findByFileHash(String fileHash);
    List<ExcelImportJob> findByStatus(ImportJobStatus status);
    boolean existsByFileHashAndStatus(String fileHash, ImportJobStatus status);
    boolean existsByFileHashAndStatusAndIdNot(String fileHash, ImportJobStatus status, UUID id);
    List<ExcelImportJob> findByFileHashOrderByCreatedAtDesc(String fileHash);
    Optional<ExcelImportJob> findTopByFileHashOrderByCreatedAtDesc(String fileHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM ExcelImportJob j WHERE j.id = :id")
    Optional<ExcelImportJob> findByIdForUpdate(@Param("id") UUID id);
}
