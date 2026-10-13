package com.alexabeauty.reporting.dto;

/**
 * Policy defining how conflicts and rejected rows are handled during commit.
 */
public enum ConflictPolicy {
    /**
     * Abort the entire import if any row in the file was rejected or failed validation.
     */
    CANCEL_ON_ERROR,

    /**
     * Skip duplicate or invalid rows and commit only valid products.
     */
    SKIP_DUPLICATES
}
