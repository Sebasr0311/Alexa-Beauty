package com.alexabeauty.catalog;

import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.repository.*;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
import com.alexabeauty.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ProductAdminRegressionTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users;
 @Autowired JwtTokenProvider tokens; @Autowired BrandRepository brands; @Autowired CategoryRepository categories;
 @Autowired ProductRepository products; @Autowired ProductVariantRepository variants;
 @Autowired InventoryMovementRepository movements;
 @Autowired jakarta.persistence.EntityManager entityManager;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
 @Autowired com.alexabeauty.catalog.service.CatalogService catalog;
 String token;
 @BeforeEach void auth(){token="Bearer "+tokens.generateToken(users.findByEmail("admin@alexabeauty.com").orElseThrow());}
 JsonNode create() throws Exception {
  String sku="FIX-"+UUID.randomUUID().toString().substring(0,8);
  var body=Map.of("name",sku,"sku",sku,"brandId",brands.findAll().getFirst().getId(),"categoryId",categories.findAll().getFirst().getId(),"salePrice",100,"referenceCost",40,"stockOnHand",5);
  return json.readTree(mvc.perform(post("/api/v1/admin/products").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("data");
 }
 @Test void defaultCostPersistsAndIsPrivate() throws Exception {
  var p=create(); assertThat(p.path("variants").get(0).path("referenceCost").decimalValue()).isEqualByComparingTo("40");
  mvc.perform(get("/api/v1/catalog/products/"+p.path("slug").asText())).andExpect(status().isOk()).andExpect(jsonPath("$.data.variants[0].referenceCost").doesNotExist());
 }
 @Test void deleteHidesOperationalProductButPreservesLedger() throws Exception {
  var p=create(); var id=UUID.fromString(p.path("id").asText()); var variant=UUID.fromString(p.path("variants").get(0).path("id").asText());
  mvc.perform(delete("/api/v1/admin/products/"+id).header("Authorization",token)).andExpect(status().isOk());
  assertThat(products.findById(id)).isPresent(); assertThat(movements.findByVariantIdOrderByCreatedAtDesc(variant)).hasSize(1);
  mvc.perform(get("/api/v1/admin/products").param("query",p.path("sku").asText()).header("Authorization",token)).andExpect(jsonPath("$.data.totalElements").value(0));
  mvc.perform(get("/api/v1/admin/products/"+id).header("Authorization",token)).andExpect(status().isNotFound());
  mvc.perform(patch("/api/v1/admin/products/"+id+"/status").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}")).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/admin/inventory/summary").param("query",p.path("sku").asText()).header("Authorization",token)).andExpect(jsonPath("$.data.totalElements").value(0));
 }
 @Test void reservedProductCannotBeDeletedOrLoseReservedStock() throws Exception {
  var p=create();var id=UUID.fromString(p.path("id").asText());var v=variants.findById(UUID.fromString(p.path("variants").get(0).path("id").asText())).orElseThrow();v.setStockReserved(1);variants.saveAndFlush(v);
  mvc.perform(delete("/api/v1/admin/products/"+id).header("Authorization",token)).andExpect(status().isConflict());
  assertThat(products.findById(id).orElseThrow().isActive()).isTrue();assertThat(v.getStockReserved()).isEqualTo(1);assertThat(v.getStockOnHand()).isEqualTo(5);
 }
 @Test void arbitraryBrandCanBeCreatedAndAssignedThroughMetadataEdit() throws Exception {
  var p=create();String name="Marca libre "+UUID.randomUUID();
  var brand=json.readTree(mvc.perform(post("/api/v1/admin/brands").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("name",name)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("data");
  mvc.perform(put("/api/v1/admin/products/"+p.path("id").asText()).header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("name","Nombre actualizado","brandId",brand.path("id").asText(),"categoryId",p.path("category").path("id").asText(),"description","Descripción actualizada")))).andExpect(status().isOk()).andExpect(jsonPath("$.data.brand.name").value(name)).andExpect(jsonPath("$.data.name").value("Nombre actualizado"));
 }
 @Test void uploadedPhotoSurvivesFreshReadAndBecomesListCover() throws Exception {
  var p=create();var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
  var file=new org.springframework.mock.web.MockMultipartFile("file","cover.png","image/png",bytes.toByteArray());
  var image=json.readTree(mvc.perform(multipart("/api/v1/admin/products/"+p.path("id").asText()+"/images").file(file).param("isCover","true").header("Authorization",token)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("data");
  entityManager.flush();entityManager.clear();
  mvc.perform(get("/api/v1/admin/products/"+p.path("id").asText()).header("Authorization",token)).andExpect(jsonPath("$.data.images[0].id").value(image.path("id").asText()));
  mvc.perform(get("/api/v1/admin/products").param("query",p.path("sku").asText()).header("Authorization",token)).andExpect(jsonPath("$.data.content[0].coverImageUrl").value(image.path("url").asText()));
 }
 @Test void negativeCostIsRejected() throws Exception {
  var p=create();String variant=p.path("variants").get(0).path("id").asText();
  mvc.perform(put("/api/v1/admin/products/"+p.path("id").asText()+"/variants/"+variant).header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content("{\"salePrice\":100,\"referenceCost\":-1}")).andExpect(status().isBadRequest());
 }

 @Test void soldOrderAndReceiptRemainReadableAfterDeletion() throws Exception {
  var p=create();var productId=UUID.fromString(p.path("id").asText());var variant=variants.findById(UUID.fromString(p.path("variants").get(0).path("id").asText())).orElseThrow();
  var order=com.alexabeauty.order.domain.Order.builder().reference("HISTORY-"+UUID.randomUUID().toString().substring(0,8)).customerName("Historic customer").customerPhone("3001112233").deliveryType("RETIRO_TIENDA").subtotal(new java.math.BigDecimal("100")).total(new java.math.BigDecimal("100")).status(com.alexabeauty.order.domain.OrderStatus.PAGADO).paidAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(60)).trackingTokenHash("historic-test").build();
  var item=com.alexabeauty.order.domain.OrderItem.builder().order(order).variant(variant).sku(variant.getSku()).productName("Frozen product name").quantity(1).unitPrice(new java.math.BigDecimal("100")).subtotal(new java.math.BigDecimal("100")).costSnapshot(new java.math.BigDecimal("40")).build();order.getItems().add(item);entityManager.persist(order);
  var receipt=com.alexabeauty.inventory.domain.StockReceipt.builder().receiptNumber("HIST-"+UUID.randomUUID().toString().substring(0,8)).totalUnits(1).build();var receiptItem=com.alexabeauty.inventory.domain.StockReceiptItem.builder().receipt(receipt).variant(variant).quantity(1).unitCost(new java.math.BigDecimal("40")).build();receipt.getItems().add(receiptItem);entityManager.persist(receipt);entityManager.flush();
  mvc.perform(delete("/api/v1/admin/products/"+productId).header("Authorization",token)).andExpect(status().isOk());entityManager.flush();entityManager.clear();
  var saved=entityManager.find(com.alexabeauty.order.domain.OrderItem.class,item.getId());assertThat(saved.getProductName()).isEqualTo("Frozen product name");assertThat(saved.getCostSnapshot()).isEqualByComparingTo("40");assertThat(saved.getVariant().getProduct().isDeleted()).isTrue();
  mvc.perform(get("/api/v1/admin/orders/"+order.getId()).header("Authorization",token)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/admin/inventory/receipts/"+receipt.getId()).header("Authorization",token)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/admin/inventory/movements").param("variantId",variant.getId().toString()).header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
 }
 @Test void deletedProductRejectsOperationalMutationsAndRepeatDeleteIsSafe() throws Exception {
  var p=create();String id=p.path("id").asText();String variant=p.path("variants").get(0).path("id").asText();
  mvc.perform(delete("/api/v1/admin/products/"+id).header("Authorization",token)).andExpect(status().isOk());
  mvc.perform(delete("/api/v1/admin/products/"+id).header("Authorization",token)).andExpect(status().isOk());
  mvc.perform(put("/api/v1/admin/products/"+id+"/variants/"+variant).header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content("{\"salePrice\":100}")).andExpect(status().isNotFound());
  mvc.perform(post("/api/v1/admin/inventory/adjustments").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("variantId",variant,"newStockOnHand",4,"reason","Test reason")))).andExpect(status().isConflict());
 }

 @Test void clearingCostDoesNotChangeExistingSaleSnapshot() throws Exception {
  var p=create();String id=p.path("id").asText(),v=p.path("variants").get(0).path("id").asText();
  mvc.perform(put("/api/v1/admin/products/"+id+"/variants/"+v).header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content("{\"salePrice\":100,\"clearReferenceCost\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.referenceCost").doesNotExist());
  assertThat(variants.findById(UUID.fromString(v)).orElseThrow().getReferenceCost()).isNull();
 }

 @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
 void deleteSerializesAgainstMetadataAndStatusWriters() throws Exception {
  for(boolean statusUpdate : new boolean[]{false,true}) {
   var product=create();UUID id=UUID.fromString(product.path("id").asText());
   var held=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
   var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
   try {
    var deleting=pool.submit(()->new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(tx->{catalog.deleteProduct(id);entityManager.flush();held.countDown();try{if(!release.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Lock test timed out");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}return null;}));
    assertThat(held.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    var mutation=pool.submit(()->mvc.perform((statusUpdate?patch("/api/v1/admin/products/"+id+"/status").content("{\"active\":true}"):put("/api/v1/admin/products/"+id).content("{\"name\":\"Must not resurrect\"}")).header("Authorization",token).contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse().getStatus());
    org.junit.jupiter.api.Assertions.assertThrows(java.util.concurrent.TimeoutException.class,()->mutation.get(150,java.util.concurrent.TimeUnit.MILLISECONDS));
    release.countDown();deleting.get(10,java.util.concurrent.TimeUnit.SECONDS);assertThat(mutation.get(10,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(404);
    assertThat(products.findById(id).orElseThrow().isDeleted()).isTrue();
   } finally {release.countDown();pool.shutdownNow();}
  }
 }

}
