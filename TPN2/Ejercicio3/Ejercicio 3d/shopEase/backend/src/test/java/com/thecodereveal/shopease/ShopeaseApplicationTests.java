package com.thecodereveal.shopease;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ShopeaseApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Test void adminCanUploadImage() throws Exception {
        String login=mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"userName\":\"admin@shopease.local\",\"password\":\"Admin123!\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token=json.readTree(login).get("token").asText();
        mvc.perform(multipart("/api/file").file(new org.springframework.mock.web.MockMultipartFile("file", "integration-test.txt", "text/plain", "demo".getBytes())).param("fileName", "integration-test.txt").header("Authorization", "Bearer "+token)).andExpect(status().isCreated());
        mvc.perform(get("/uploads/integration-test.txt")).andExpect(status().isOk()).andExpect(content().string("demo"));
    }
    @Test void catalogLoginAndCashOrder() throws Exception {
        JsonNode catalog=json.readTree(mvc.perform(get("/api/products")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(8,catalog.size());
        JsonNode product=json.readTree(mvc.perform(get("/api/products/"+catalog.get(0).get("id").asText())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String login=mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"userName\":\"demo@shopease.local\",\"password\":\"Demo123!\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String auth="Bearer "+json.readTree(login).get("token").asText();
        JsonNode profile=json.readTree(mvc.perform(get("/api/user/profile").header("Authorization",auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String request=String.format("{\"addressId\":\"%s\",\"paymentMethod\":\"COD\",\"totalAmount\":0.01,\"orderItemRequests\":[{\"productId\":\"%s\",\"productVariantId\":\"%s\",\"quantity\":1}]}",profile.get("addressList").get(0).get("id").asText(),product.get("id").asText(),product.get("variants").get(0).get("id").asText());
        mvc.perform(post("/api/order").header("Authorization",auth).contentType("application/json").content(request)).andExpect(status().isOk()).andExpect(jsonPath("$.orderId").exists());
        mvc.perform(get("/api/order/user").header("Authorization",auth)).andExpect(status().isOk()).andExpect(jsonPath("$[0].totalAmount").value(product.get("price").asDouble()));
        mvc.perform(post("/api/products").header("Authorization",auth).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/user/profile")).andExpect(status().isUnauthorized());
    }
}
