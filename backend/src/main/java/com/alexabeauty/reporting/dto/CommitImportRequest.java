package com.alexabeauty.reporting.dto;

/**
 * Request body for committing an Excel import job.
 */
public record CommitImportRequest(
        ConflictPolicy policy
) {
}
