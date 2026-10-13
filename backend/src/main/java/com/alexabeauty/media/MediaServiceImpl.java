package com.alexabeauty.media;

import com.alexabeauty.catalog.domain.Product;
import com.alexabeauty.catalog.domain.ProductImage;
import com.alexabeauty.catalog.repository.ProductImageRepository;
import com.alexabeauty.catalog.repository.ProductRepository;
import com.alexabeauty.common.error.ApiException;
import com.alexabeauty.common.error.ErrorCodes;
import com.alexabeauty.common.error.ResourceNotFoundException;
import com.alexabeauty.common.error.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class MediaServiceImpl implements MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaServiceImpl.class);

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            ".exe", ".bat", ".cmd", ".sh", ".php", ".jsp", ".py", ".pl", ".dll", ".jar", ".msi", ".scr", ".bin"
    );

    private final StorageGateway storageGateway;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    public MediaServiceImpl(
            StorageGateway storageGateway,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository
    ) {
        this.storageGateway = storageGateway;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
    }

    @Override
    public ProductImage uploadProductImage(UUID productId, MultipartFile file, String altText, Boolean isCover) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        byte[] bytes = validateFile(file);

        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String ext = resolveExtension(contentType);
        String storagePath = String.format("products/%s/%s.%s", productId, UUID.randomUUID(), ext);

        storageGateway.upload(bytes, storagePath, contentType);

        List<ProductImage> existingImages = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
        boolean shouldBeCover = existingImages.isEmpty() || Boolean.TRUE.equals(isCover);

        if (shouldBeCover) {
            for (ProductImage img : existingImages) {
                if (img.isCover()) {
                    img.setCover(false);
                    productImageRepository.save(img);
                }
            }
        }

        ProductImage productImage = ProductImage.builder()
                .product(product)
                .storagePath(storagePath)
                .altText(altText != null && !altText.isBlank() ? altText.trim() : product.getName())
                .sortOrder(existingImages.size())
                .isCover(shouldBeCover)
                .build();

        return productImageRepository.save(productImage);
    }

    @Override
    public void deleteProductImage(UUID productId, UUID imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("ProductImage", imageId);
        }

        boolean wasCover = image.isCover();
        storageGateway.delete(image.getStoragePath());
        productImageRepository.delete(image);

        if (wasCover) {
            List<ProductImage> remaining = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
            if (!remaining.isEmpty()) {
                ProductImage nextCover = remaining.get(0);
                nextCover.setCover(true);
                productImageRepository.save(nextCover);
            }
        }
    }

    @Override
    public ProductImage setCoverImage(UUID productId, UUID imageId) {
        ProductImage target = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));

        if (!target.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("ProductImage", imageId);
        }

        List<ProductImage> images = productImageRepository.findByProductId(productId);
        for (ProductImage img : images) {
            img.setCover(img.getId().equals(imageId));
        }
        productImageRepository.saveAll(images);

        target.setCover(true);
        return target;
    }

    @Override
    @Transactional(readOnly = true)
    public String getPublicUrl(String storagePath) {
        return storageGateway.getPublicUrl(storagePath);
    }

    private byte[] validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("El archivo de imagen no puede estar vacío");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("El tamaño del archivo excede el límite máximo de 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ValidationException(
                    "Tipo de archivo no permitido: " + contentType + ". Tipos válidos: image/jpeg, image/png"
            );
        }

        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            String lowerName = originalName.toLowerCase(Locale.ROOT);
            for (String forbiddenExt : FORBIDDEN_EXTENSIONS) {
                if (lowerName.endsWith(forbiddenExt)) {
                    throw new ValidationException("Extensiones ejecutables o peligrosas no están permitidas: " + originalName);
                }
            }
        }

        try (var input = javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(file.getBytes()))) {
            var readers = javax.imageio.ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ValidationException("El contenido no es una imagen JPEG o PNG válida.");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!(format.equals("jpeg") || format.equals("png"))
                        || !contentType.equals(format.equals("jpeg") ? "image/jpeg" : "image/png")) {
                    throw new ValidationException("El formato real de la imagen no coincide con el tipo declarado.");
                }
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 16000000L) {
                    throw new ValidationException("La imagen excede 16 megapíxeles.");
                }
                var image = reader.read(0);
                var output = new java.io.ByteArrayOutputStream();
                if (image == null || !javax.imageio.ImageIO.write(image, format, output)) {
                    throw new ValidationException("No se pudo decodificar la imagen.");
                }
                if (output.size() > MAX_FILE_SIZE) throw new ValidationException("La imagen procesada excede 5MB.");
                return output.toByteArray();
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException exception) {
            throw new ValidationException("El contenido de la imagen es inválido.");
        }
    }

    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
