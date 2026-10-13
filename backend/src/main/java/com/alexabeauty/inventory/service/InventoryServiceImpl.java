package com.alexabeauty.inventory.service;

import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.catalog.repository.ProductVariantRepository;
import com.alexabeauty.common.audit.AuditLog;
import com.alexabeauty.common.audit.AuditLogRepository;
import com.alexabeauty.common.error.ApiException;
import com.alexabeauty.common.error.ResourceNotFoundException;
import com.alexabeauty.inventory.domain.InventoryMovement;
import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.domain.StockReceipt;
import com.alexabeauty.inventory.domain.StockReceiptItem;
import com.alexabeauty.inventory.dto.CreateStockReceiptRequest;
import com.alexabeauty.inventory.dto.InventoryMovementResponse;
import com.alexabeauty.inventory.dto.InventorySummaryResponse;
import com.alexabeauty.inventory.dto.StockAdjustmentRequest;
import com.alexabeauty.inventory.dto.StockAdjustmentResponse;
import com.alexabeauty.inventory.dto.StockReceiptItemRequest;
import com.alexabeauty.inventory.dto.StockReceiptResponse;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
import com.alexabeauty.inventory.repository.StockReceiptItemRepository;
import com.alexabeauty.inventory.repository.StockReceiptRepository;
import com.alexabeauty.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);
    private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final ProductVariantRepository productVariantRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final StockReceiptRepository stockReceiptRepository;
    private final StockReceiptItemRepository stockReceiptItemRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public InventoryServiceImpl(
            ProductVariantRepository productVariantRepository,
            InventoryMovementRepository inventoryMovementRepository,
            StockReceiptRepository stockReceiptRepository,
            StockReceiptItemRepository stockReceiptItemRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.productVariantRepository = productVariantRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.stockReceiptRepository = stockReceiptRepository;
        this.stockReceiptItemRepository = stockReceiptItemRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public StockReceiptResponse createReceipt(CreateStockReceiptRequest request, String idempotencyKey, UserPrincipal actor) {
        log.info("Processing goods receipt with idempotencyKey='{}', requestReceiptNumber='{}'",
                idempotencyKey, request.receiptNumber());

        // 1. Idempotency Check: if idempotencyKey or receiptNumber already exists, return existing receipt without mutating stock
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<StockReceipt> existingByKey = stockReceiptRepository.findByReceiptNumber(idempotencyKey.trim());
            if (existingByKey.isPresent()) {
                log.info("Duplicate receipt request detected by idempotencyKey '{}'. Returning existing receipt id {}",
                        idempotencyKey, existingByKey.get().getId());
                return StockReceiptResponse.from(existingByKey.get());
            }

            // Check if stored in notes
            Optional<StockReceipt> existingByNotes = stockReceiptRepository.findAll().stream()
                    .filter(r -> r.getNotes() != null && r.getNotes().contains("[Idempotency-Key: " + idempotencyKey.trim() + "]"))
                    .findFirst();
            if (existingByNotes.isPresent()) {
                log.info("Duplicate receipt request detected by notes key '{}'. Returning existing receipt id {}",
                        idempotencyKey, existingByNotes.get().getId());
                return StockReceiptResponse.from(existingByNotes.get());
            }
        }

        if (request.receiptNumber() != null && !request.receiptNumber().isBlank()) {
            Optional<StockReceipt> existingByReceiptNumber = stockReceiptRepository.findByReceiptNumber(request.receiptNumber().trim());
            if (existingByReceiptNumber.isPresent()) {
                log.info("Duplicate receipt request detected by receiptNumber '{}'. Returning existing receipt id {}",
                        request.receiptNumber(), existingByReceiptNumber.get().getId());
                return StockReceiptResponse.from(existingByReceiptNumber.get());
            }
        }

        // 2. Resolve receipt number
        String receiptNumber;
        if (request.receiptNumber() != null && !request.receiptNumber().isBlank()) {
            receiptNumber = request.receiptNumber().trim();
        } else if (idempotencyKey != null && !idempotencyKey.isBlank() && idempotencyKey.trim().length() <= 50) {
            receiptNumber = idempotencyKey.trim();
        } else {
            receiptNumber = generateReceiptNumber();
        }

        // 3. Resolve notes with idempotency tag
        String notes = request.notes();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String idTag = "[Idempotency-Key: " + idempotencyKey.trim() + "]";
            if (notes == null || notes.isBlank()) {
                notes = idTag;
            } else if (!notes.contains(idTag)) {
                notes = idTag + " " + notes.trim();
            }
        }

        // 4. Resolve actor user entity
        User actorEntity = null;
        if (actor != null && actor.getId() != null) {
            actorEntity = userRepository.findById(actor.getId()).orElse(null);
        }

        // 5. Deterministic ID order for deadlock-free multi-variant locking
        List<UUID> sortedVariantIds = request.items().stream()
                .map(StockReceiptItemRequest::variantId)
                .distinct()
                .sorted()
                .toList();

        List<ProductVariant> lockedVariants = productVariantRepository.findByIdInOrderByIdForUpdate(sortedVariantIds);
        Map<UUID, ProductVariant> variantMap = lockedVariants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        for (StockReceiptItemRequest itemReq : request.items()) {
            if (!variantMap.containsKey(itemReq.variantId())) {
                throw new ResourceNotFoundException("ProductVariant", itemReq.variantId());
            }
        }

        // 6. Build StockReceipt entity
        StockReceipt receipt = StockReceipt.builder()
                .receiptNumber(receiptNumber)
                .providerName(request.providerName())
                .invoiceNumber(request.invoiceNumber())
                .notes(notes)
                .user(actorEntity)
                .items(new ArrayList<>())
                .build();

        int totalUnits = 0;
        BigDecimal totalCost = BigDecimal.ZERO;

        // 7. Process each item: update stockOnHand and record ENTRADA movement
        for (StockReceiptItemRequest itemReq : request.items()) {
            ProductVariant variant = variantMap.get(itemReq.variantId());
            int qtyBefore = variant.getStockOnHand();
            int qtyAfter = qtyBefore + itemReq.quantity();

            // NEVER touch stock_reserved on receipt!
            variant.setStockOnHand(qtyAfter);
            productVariantRepository.save(variant);

            // Create ENTRADA movement
            InventoryMovement movement = InventoryMovement.builder()
                    .variant(variant)
                    .movementType(MovementType.ENTRADA)
                    .quantityDelta(itemReq.quantity())
                    .qtyBefore(qtyBefore)
                    .qtyAfter(qtyAfter)
                    .reference(receiptNumber)
                    .reason("Recepción de mercancía")
                    .user(actorEntity)
                    .build();
            inventoryMovementRepository.save(movement);

            // Create StockReceiptItem
            StockReceiptItem item = StockReceiptItem.builder()
                    .receipt(receipt)
                    .variant(variant)
                    .quantity(itemReq.quantity())
                    .unitCost(itemReq.unitCost())
                    .notes(itemReq.notes())
                    .build();
            receipt.getItems().add(item);

            totalUnits += itemReq.quantity();
            if (itemReq.unitCost() != null) {
                totalCost = totalCost.add(itemReq.unitCost().multiply(BigDecimal.valueOf(itemReq.quantity())));
            }
        }

        receipt.setTotalUnits(totalUnits);
        receipt.setTotalCost(totalCost);
        StockReceipt savedReceipt = stockReceiptRepository.save(receipt);

        log.info("Goods receipt created successfully: receiptNumber={}, totalUnits={}, totalCost={}",
                savedReceipt.getReceiptNumber(), savedReceipt.getTotalUnits(), savedReceipt.getTotalCost());

        return StockReceiptResponse.from(savedReceipt);
    }

    @Override
    @Transactional
    public StockAdjustmentResponse adjustStock(StockAdjustmentRequest request, UserPrincipal actor) {
        log.info("Adjusting stock for variantId={} to newStockOnHand={}, reason='{}'",
                request.variantId(), request.newStockOnHand(), request.reason());

        // 1. Lock variant
        ProductVariant variant = productVariantRepository.findByIdForUpdate(request.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", request.variantId()));

        // 2. INVIOLABLE BUSINESS INVARIANT: newStockOnHand cannot be less than stockReserved
        if (request.newStockOnHand() < variant.getStockReserved()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El nuevo stock físico (" + request.newStockOnHand() + ") no puede ser inferior a las unidades actualmente reservadas (" + variant.getStockReserved() + ")");
        }

        int previousStockOnHand = variant.getStockOnHand();
        int delta = request.newStockOnHand() - previousStockOnHand;
        int stockReserved = variant.getStockReserved();
        int availableStock = request.newStockOnHand() - stockReserved;
        String productName = variant.getProduct() != null ? variant.getProduct().getName() : null;

        // If delta == 0, return current summary without creating movement
        if (delta == 0) {
            return new StockAdjustmentResponse(
                    variant.getId(),
                    variant.getSku(),
                    productName,
                    previousStockOnHand,
                    request.newStockOnHand(),
                    stockReserved,
                    availableStock,
                    0,
                    request.reason(),
                    variant.getUpdatedAt()
            );
        }

        // 3. Resolve actor user entity
        User actorEntity = null;
        if (actor != null && actor.getId() != null) {
            actorEntity = userRepository.findById(actor.getId()).orElse(null);
        }

        // 4. Update variant stockOnHand
        variant.setStockOnHand(request.newStockOnHand());
        ProductVariant savedVariant = productVariantRepository.save(variant);

        // 5. Save AJUSTE movement
        InventoryMovement movement = InventoryMovement.builder()
                .variant(savedVariant)
                .movementType(MovementType.AJUSTE)
                .quantityDelta(delta)
                .qtyBefore(previousStockOnHand)
                .qtyAfter(request.newStockOnHand())
                .reference("AJUSTE_MANUAL")
                .reason(request.reason())
                .user(actorEntity)
                .build();
        inventoryMovementRepository.save(movement);

        // 6. Log to AuditLog
        AuditLog auditLog = AuditLog.builder()
                .actorEmail(actor != null ? actor.getUsername() : "system")
                .action("STOCK_ADJUSTMENT")
                .entityType("ProductVariant")
                .entityId(savedVariant.getId().toString())
                .beforeSnapshot("{\"stockOnHand\":" + previousStockOnHand + ",\"stockReserved\":" + stockReserved + "}")
                .afterSnapshot("{\"stockOnHand\":" + request.newStockOnHand() + ",\"stockReserved\":" + stockReserved + ",\"delta\":" + delta + ",\"reason\":\"" + request.reason() + "\"}")
                .build();
        auditLogRepository.save(auditLog);

        log.info("Stock adjusted successfully for variant sku={}, delta={}, newStockOnHand={}",
                savedVariant.getSku(), delta, savedVariant.getStockOnHand());

        return new StockAdjustmentResponse(
                savedVariant.getId(),
                savedVariant.getSku(),
                productName,
                previousStockOnHand,
                savedVariant.getStockOnHand(),
                savedVariant.getStockReserved(),
                availableStock,
                delta,
                request.reason(),
                savedVariant.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> getKardexMovements(
            UUID variantId,
            MovementType type,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        Specification<InventoryMovement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (variantId != null) {
                predicates.add(cb.equal(root.get("variant").get("id"), variantId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("movementType"), type));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<InventoryMovement> page = inventoryMovementRepository.findAll(spec, pageable);
        return page.map(InventoryMovementResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventorySummaryResponse> getInventorySummary(
            String query,
            Boolean lowStockOnly,
            Pageable pageable
    ) {
        String trimmedQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        boolean lowStockFilter = Boolean.TRUE.equals(lowStockOnly);

        Page<ProductVariant> variantsPage = productVariantRepository.findInventorySummary(
                trimmedQuery,
                lowStockFilter,
                pageable
        );

        return variantsPage.map(InventorySummaryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public StockReceiptResponse getReceiptById(UUID id) {
        StockReceipt receipt = stockReceiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockReceipt", id));
        return StockReceiptResponse.from(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockReceiptResponse> listReceipts(Pageable pageable) {
        return stockReceiptRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(StockReceiptResponse::from);
    }

    private String generateReceiptNumber() {
        String datePart = RECEIPT_DATE_FORMATTER.format(Instant.now());
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return "REC-" + datePart + "-" + randomSuffix;
    }
}
