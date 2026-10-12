package com.alexabeauty.media;

public interface StorageGateway {

    /**
     * Uploads content to the specified storage path.
     *
     * @param content byte content of the file
     * @param path relative path where the file will be stored
     * @param contentType MIME type of the file
     * @return the stored path or identifier
     */
    String upload(byte[] content, String path, String contentType);

    /**
     * Deletes the file at the specified storage path.
     *
     * @param path relative storage path
     */
    void delete(String path);

    /**
     * Gets the publicly accessible URL for the specified storage path.
     *
     * @param path relative storage path
     * @return full public URL or web path
     */
    String getPublicUrl(String path);
}
