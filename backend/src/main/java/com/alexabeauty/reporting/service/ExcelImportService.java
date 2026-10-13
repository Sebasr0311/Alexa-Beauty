package com.alexabeauty.reporting.service;

import com.alexabeauty.reporting.dto.ConflictPolicy;
import com.alexabeauty.reporting.dto.ExcelCommitResponse;
import com.alexabeauty.reporting.dto.ExcelImportPreviewResponse;
import com.alexabeauty.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Service for Excel product template generation, preview diagnostics, and atomic commit.
 */
public interface ExcelImportService {

    /**
     * Generates the official ALEXA_PRODUCTS_V1 template workbook bytes.
     */
    byte[] generateTemplate();

    /**
     * Parses and validates an uploaded Excel file, persisting a preview job and errors.
     */
    ExcelImportPreviewResponse processPreview(MultipartFile file, UserPrincipal principal);

    /**
     * Atomically commits valid rows from a previewed import job into catalog and inventory.
     */
    ExcelCommitResponse commitImport(UUID jobId, ConflictPolicy policy, UserPrincipal principal);

    /**
     * Generates an Excel workbook containing only rejected rows with the ERROR_MOTIVO diagnostic column.
     */
    byte[] generateRejectedRowsExcel(UUID jobId);
}
