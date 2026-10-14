package com.alexabeauty.catalog;

import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.repository.*;
import com.alexabeauty.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class VisualShadeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired JwtTokenProvider jwt;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired ProductVariantRepository variants;
    String token;

    @BeforeEach void auth() { token = jwt.generateToken(users.findByEmail("admin@alexabeauty.com").orElseThrow()); }
    JsonNode product() throws Exception {
        var body = Map.of("name", "Visual " + UUID.randomUUID(), "sku", "VIS-" + UUID.randomUUID(),
            "brandId", brands.findBySlug("bella-cosmetics").orElseThrow().getId(),
            "categoryId", categories.findBySlug("rostro").orElseThrow().getId(), "salePrice", 25000, "stockOnHand", 2);
        return json.readTree(mvc.perform(post("/api/v1/admin/products").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("data");
    }
    String path(JsonNode p) { return "/api/v1/admin/products/" + p.get("id").asText() + "/variants"; }
    Map<String,Object> shade(String code, String hex) {
        return new HashMap<>(Map.of("shadeCode", code, "shadeName", "Classic Ivory", "shadeHex", hex,
            "presentation", "30 ml", "salePrice", 30000, "stockOnHand", 4, "minStock", 1));
    }
    JsonNode add(JsonNode p, Map<String,Object> body) throws Exception {
        return json.readTree(mvc.perform(post(path(p)).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.shadeHex").value("#AABBCC")).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("data");
    }
    @Test void addEditAndDeactivatePreservesStockAndExistingVariant() throws Exception {
        var p=product(); var before=p.get("variants").get(0); var v=add(p,shade("110","#aabbcc"));
        assertThat(v.get("sku").asText()).isNotBlank();
        var edit=shade("110","#AABBCC"); edit.remove("stockOnHand"); edit.put("salePrice",32000);
        mvc.perform(put(path(p)+"/"+v.get("id").asText()).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(edit))).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.stockOnHand").value(4)).andExpect(jsonPath("$.data.salePrice").value(32000));
        edit.put("stockOnHand",99);
        mvc.perform(put(path(p)+"/"+v.get("id").asText()).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(edit))).andExpect(status().isBadRequest());
        mvc.perform(patch(path(p)+"/"+v.get("id").asText()+"/status").header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalog/products/"+p.get("slug").asText())).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.variants.length()").value(1));
        assertThat(variants.findById(UUID.fromString(before.get("id").asText())).orElseThrow().getStockOnHand()).isEqualTo(2);
    }
    @Test void invalidHexAndDuplicateCombinationRejectedButDifferentSizeAllowed() throws Exception {
        var p=product(); add(p,shade("110","#AABBCC"));
        mvc.perform(post(path(p)).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(shade("115","red")))).andExpect(status().isBadRequest());
        mvc.perform(post(path(p)).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(shade(" 110 ","#AABBCC")))).andExpect(status().isConflict());
        var other=shade("110","#AABBCC"); other.put("presentation","15 ml"); add(p,other);
    }
    @Test void nestedRoutesRequireAuthenticationAndProductOwnership() throws Exception {
        var p=product(); var other=product(); var v=add(p,shade("110","#AABBCC"));
        mvc.perform(get(path(p))).andExpect(status().isUnauthorized());
        var edit=shade("110","#AABBCC"); edit.remove("stockOnHand");
        mvc.perform(put(path(other)+"/"+v.get("id").asText()).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(edit))).andExpect(status().isNotFound());
    }

    @Test void twoShadesCheckoutSnapshotsAndCashOnDeliveryStayCompatible() throws Exception {
        var p=product(); var a=add(p,shade("107","#AABBCC")); var b=add(p,shade("110","#AABBCC"));
        var body=Map.of("customerName","Shade acceptance", "customerPhone","573001234567", "deliveryType","DOMICILIO",
            "items",List.of(Map.of("variantId",a.get("id").asText(),"quantity",1),Map.of("variantId",b.get("id").asText(),"quantity",2)));
        var result=json.readTree(mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(body))).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.items.length()").value(2)).andExpect(jsonPath("$.data.total").value(90000))
            .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("data");
        assertThat(result.get("whatsappMessageText").asText()).contains("107", "110", "Classic Ivory", "30 ml");
        String oldLabel=result.get("items").get(0).get("variantName").asText();
        var edit=shade("107","#AABBCC");edit.remove("stockOnHand");edit.put("shadeName","Changed for future orders");
        mvc.perform(put(path(p)+"/"+a.get("id").asText()).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(edit))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/orders/"+result.get("orderId").asText()).header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].variantName").value(oldLabel));
        mvc.perform(post("/api/v1/admin/orders/"+result.get("orderId").asText()+"/transition")
            .header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
            .content("{\"newStatus\":\"ENTREGADO\",\"notes\":\"Cash collected in local test\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ENTREGADO"));
        assertThat(variants.findById(UUID.fromString(a.get("id").asText())).orElseThrow().getStockOnHand()).isEqualTo(3);
        assertThat(variants.findById(UUID.fromString(b.get("id").asText())).orElseThrow().getStockOnHand()).isEqualTo(2);
    }
    @Test void inactiveAndExhaustedVariantsCannotCheckout() throws Exception {
        var p=product();var a=add(p,shade("110","#AABBCC"));
        mvc.perform(patch(path(p)+"/"+a.get("id").asText()+"/status").header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isOk());
        var body=Map.of("customerName","Test unavailable", "customerPhone","573001234567", "deliveryType","LOCAL_PICKUP",
            "items",List.of(Map.of("variantId",a.get("id").asText(),"quantity",1)));
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
            .andExpect(status().isConflict());
        var exhausted=shade("115","#AABBCC");exhausted.put("stockOnHand",0);var b=add(p,exhausted);
        body=new HashMap<>(body);body.put("items",List.of(Map.of("variantId",b.get("id").asText(),"quantity",1)));
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
            .andExpect(status().isConflict());
    }
    @Test void imageOwnershipAndGeneralCoverArePreserved() throws Exception {
        var p=product();var other=product();var v=add(p,shade("110","#AABBCC"));
        var image=new java.awt.image.BufferedImage(3,3,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",output);
        var file=new org.springframework.mock.web.MockMultipartFile("file","tone.png","image/png",output.toByteArray());
        mvc.perform(multipart("/api/v1/admin/products/"+other.get("id").asText()+"/images").file(file)
            .param("variantId",v.get("id").asText()).header("Authorization","Bearer "+token)).andExpect(status().isNotFound());
        var general=json.readTree(mvc.perform(multipart("/api/v1/admin/products/"+p.get("id").asText()+"/images")
            .file(file).param("isCover","true").header("Authorization","Bearer "+token)).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("data");
        mvc.perform(multipart("/api/v1/admin/products/"+p.get("id").asText()+"/images").file(file)
            .param("variantId",v.get("id").asText()).header("Authorization","Bearer "+token)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.variantId").value(v.get("id").asText())).andExpect(jsonPath("$.data.isCover").value(false));
        mvc.perform(get("/api/v1/admin/products/"+p.get("id").asText()).header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.images[?(@.id == '"+general.get("id").asText()+"')].isCover").value(true));
    }

    @Test void concurrentSameCombinationAddsOnlyOneVariant() throws Exception {
        var p=product();var body=json.writeValueAsBytes(shade("201","#AABBCC"));
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var start=new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Integer> add=()->{start.await();return mvc.perform(post(path(p))
                .header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();};
            var first=pool.submit(add);var second=pool.submit(add);start.countDown();
            assertThat(List.of(first.get(20,java.util.concurrent.TimeUnit.SECONDS),second.get(20,java.util.concurrent.TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(201,409);
            assertThat(variants.findByProductId(UUID.fromString(p.get("id").asText()))).hasSize(2);
        } finally {pool.shutdownNow();}
    }
}
