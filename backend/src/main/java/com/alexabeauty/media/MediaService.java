package com.alexabeauty.media;

import com.alexabeauty.catalog.domain.ProductImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface MediaService {

    ProductImage uploadProductImage(UUID productId, MultipartFile file, String altText, Boolean isCover);

    void deleteProductImage(UUID productId, UUID imageId);

    ProductImage setCoverImage(UUID productId, UUID imageId);

    String getPublicUrl(String storagePath);
}
