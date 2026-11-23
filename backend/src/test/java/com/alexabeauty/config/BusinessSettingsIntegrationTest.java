package com.alexabeauty.config;

import com.alexabeauty.auth.domain.Role;
import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.domain.Brand;
import com.alexabeauty.catalog.domain.Category;
import com.alexabeauty.catalog.domain.Product;
import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.catalog.repository.BrandRepository;
import com.alexabeauty.catalog.repository.CategoryRepository;
import com.alexabeauty.catalog.repository.ProductRepository;
import com.alexabeauty.catalog.repository.ProductVariantRepository;
import com.alexabeauty.common.audit.AuditLog;
import com.alexabeauty.common.audit.AuditLogRepository;
import com.alexabeauty.config.domain.AppSetting;
import com.alexabeauty.config.dto.UpdateWhatsAppNumberRequest;
import com.alexabeauty.config.repository.AppSettingRepository;
import com.alexabeauty.config.service.BusinessSettingsServiceImpl;
import com.alexabeauty.order.dto.CreateOrderRequest;
import com.alexabeauty.order.dto.OrderItemRequest;
import com.alexabeauty.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessSettingsIntegrationTest {

    private static final String DEFAULT_PRODUCTION_WHATSAPP = "573235814497";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AppSettingRepository appSettingRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    private String adminToken;
    private String operatorToken;

    @BeforeEach
    void setUp() {
        User adminUser = userRepository.findByEmail("admin@alexabeauty.com").orElseThrow();
        adminToken = jwtTokenProvider.generateToken(adminUser);

        User operatorUser = userRepository.findByEmail("operador.settings@alexabeauty.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("operador.settings@alexabeauty.com")
                        .passwordHash(passwordEncoder.encode("Operador*12345678"))
                        .role(Role.OPERADOR)
                        .fullName("Operador Pruebas")
                        .active(true)
                        .build()));
        operatorToken = jwtTokenProvider.generateToken(operatorUser);
    }

    @AfterEach
    void restoreWhatsAppSetting() {
        AppSetting setting = appSettingRepository.findBySettingKey(BusinessSettingsServiceImpl.WHATSAPP_NUMBER_KEY)
                .orElseGet(() -> AppSetting.builder()
                        .settingKey(BusinessSettingsServiceImpl.WHATSAPP_NUMBER_KEY)
                        .description("Número oficial de WhatsApp para pedidos")
                        .build());
        setting.setSettingValue(DEFAULT_PRODUCTION_WHATSAPP);
        setting.setUpdatedAt(Instant.now());
        appSettingRepository.save(setting);
    }

    @Test
    void getPublicSettings_returnsSeededNumberAnonymously() throws Exception {
        mockMvc.perform(get("/api/v1/settings/public"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.whatsappNumber").value(DEFAULT_PRODUCTION_WHATSAPP));
    }

    @Test
    void updateWhatsAppNumber_requiresAdminRole() throws Exception {
        UpdateWhatsAppNumberRequest payload = new UpdateWhatsAppNumberRequest("3105556677");

        mockMvc.perform(put("/api/v1/admin/settings/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/settings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.whatsappNumber").value(DEFAULT_PRODUCTION_WHATSAPP));

        mockMvc.perform(put("/api/v1/admin/settings/whatsapp")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateWhatsAppNumber_asAdmin_normalizesPersistsAuditsAndUpdatesCheckoutAndTracking() throws Exception {
        // 1. Colombian 10-digit mobile without country code normalizes to 57 + 10 digits
        mockMvc.perform(put("/api/v1/admin/settings/whatsapp")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateWhatsAppNumberRequest("323 5814497"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.whatsappNumber").value("573235814497"));

        // 2. Formatted international number updates live setting and creates audit entry
        mockMvc.perform(put("/api/v1/admin/settings/whatsapp")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateWhatsAppNumberRequest("+57 310 555 6677"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.whatsappNumber").value("573105556677"))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/settings/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.whatsappNumber").value("573105556677"));

        AppSetting setting = appSettingRepository.findBySettingKey(BusinessSettingsServiceImpl.WHATSAPP_NUMBER_KEY).orElseThrow();
        List<AuditLog> audits = auditLogRepository.findByEntityTypeAndEntityId("APP_SETTING", setting.getId().toString());
        assertThat(audits).anySatisfy(audit -> {
            assertThat(audit.getAction()).isEqualTo("UPDATE_WHATSAPP_NUMBER");
            assertThat(audit.getActorEmail()).isEqualTo("admin@alexabeauty.com");
            assertThat(audit.getBeforeSnapshot()).isEqualTo("573235814497");
            assertThat(audit.getAfterSnapshot()).isEqualTo("573105556677");
        });

        // 3. Checkout and tracking immediately use the updated DB WhatsApp number
        Brand brand = brandRepository.findAll().stream().findFirst().orElseThrow();
        Category category = categoryRepository.findAll().stream().findFirst().orElseThrow();
        Product product = productRepository.save(Product.builder()
                .name("Producto Config WA")
                .sku("SKU-WA-" + UUID.randomUUID().toString().substring(0, 6))
                .slug("prod-wa-" + UUID.randomUUID())
                .brand(brand)
                .category(category)
                .active(true)
                .build());
        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .sku("VAR-WA-" + UUID.randomUUID().toString().substring(0, 6))
                .shadeName("Natural")
                .salePrice(new BigDecimal("40000.00"))
                .minStock(1)
                .stockOnHand(5)
                .stockReserved(0)
                .active(true)
                .build());

        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .customerName("Laura Test")
                .customerPhone("3001234567")
                .deliveryType("RETIRO_TIENDA")
                .items(List.of(new OrderItemRequest(variant.getId(), 1)))
                .build();

        MvcResult checkoutResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.whatsappUrl").value(Matchers.startsWith("https://wa.me/573105556677?text=")))
                .andReturn();

        JsonNode data = objectMapper.readTree(checkoutResult.getResponse().getContentAsString()).path("data");
        String reference = data.path("reference").asText();
        String trackingToken = data.path("trackingToken").asText();

        mockMvc.perform(get("/api/v1/orders/track")
                        .param("reference", reference)
                        .param("token", trackingToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.whatsappUrl").value(Matchers.startsWith("https://wa.me/573105556677?text=")));
    }

    @Test
    void updateWhatsAppNumber_rejectsInvalidNumbers() throws Exception {
        for (String invalid : List.of("", "123", "abc")) {
            mockMvc.perform(put("/api/v1/admin/settings/whatsapp")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateWhatsAppNumberRequest(invalid))))
                    .andExpect(status().isBadRequest());
        }

        AppSetting setting = appSettingRepository.findBySettingKey(BusinessSettingsServiceImpl.WHATSAPP_NUMBER_KEY).orElseThrow();
        assertThat(setting.getSettingValue()).isEqualTo(DEFAULT_PRODUCTION_WHATSAPP);
    }
}
