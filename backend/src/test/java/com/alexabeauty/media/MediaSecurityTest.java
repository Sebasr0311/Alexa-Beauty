package com.alexabeauty.media;

import com.alexabeauty.catalog.domain.Product;
import com.alexabeauty.catalog.repository.ProductRepository;
import com.alexabeauty.catalog.repository.ProductImageRepository;
import com.alexabeauty.common.error.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MediaSecurityTest {
    @Test
    void rejectsExecutableBytesClaimingToBeJpeg() {
        ProductRepository products = mock(ProductRepository.class);
        StorageGateway storage = mock(StorageGateway.class);
        UUID id = UUID.randomUUID();
        when(products.findByIdForUpdate(id)).thenReturn(Optional.of(Product.builder().id(id).build()));
        MediaServiceImpl media = new MediaServiceImpl(storage, products, mock(ProductImageRepository.class));
        MockMultipartFile fake = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "MZ executable".getBytes());
        assertThatThrownBy(() -> media.uploadProductImage(id, fake, "photo", true))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(storage);
    }
}
