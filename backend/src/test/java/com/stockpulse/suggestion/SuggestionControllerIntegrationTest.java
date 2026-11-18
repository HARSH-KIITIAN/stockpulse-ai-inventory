package com.stockpulse.suggestion;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SuggestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsManualPricingAndReorderSuggestionsAndListsThem() throws Exception {
        String productId = createProduct();

        mockMvc.perform(post("/products/{productId}/suggest-pricing", productId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.triggerReason").value("MANUAL"))
                .andExpect(jsonPath("$.recommendedPrice").value(33.00));

        mockMvc.perform(post("/products/{productId}/suggest-pricing", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/products/{productId}/suggest-reorder", productId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.triggerReason").value("MANUAL"))
                .andExpect(jsonPath("$.recommendedQuantity").value(26));

        mockMvc.perform(get("/pricing-suggestions?status=PENDING&productId={productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].productId").value(productId));

        mockMvc.perform(get("/reorder-suggestions?status=PENDING&productId={productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].productId").value(productId));
    }

    @Test
    void returnsConsistentNotFoundErrorForUnknownProduct() throws Exception {
        mockMvc.perform(post("/products/{productId}/suggest-pricing",
                        "00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void acceptsPricingAndReorderSuggestionsWithAtomicSideEffects() throws Exception {
        String productId = createProduct();

        MvcResult pricingResult = mockMvc.perform(post("/products/{productId}/suggest-pricing", productId))
                .andExpect(status().isCreated())
                .andReturn();
        long pricingId = objectMapper.readTree(pricingResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(patch("/pricing-suggestions/{suggestionId}", pricingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"ACCEPT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestion.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.product.currentPrice").value(33.00));

        MvcResult reorderResult = mockMvc.perform(post("/products/{productId}/suggest-reorder", productId))
                .andExpect(status().isCreated())
                .andReturn();
        long reorderId = objectMapper.readTree(reorderResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(patch("/reorder-suggestions/{suggestionId}", reorderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"ACCEPT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestion.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.product.stockLevel").value(30));

        mockMvc.perform(patch("/pricing-suggestions/{suggestionId}", pricingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_CONFLICT"));
    }

    @Test
    void switchesRuntimeStrategyThroughConfigurationEndpoint() throws Exception {
        mockMvc.perform(get("/config/commerce-strategy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStrategy").value("RULE_BASED"));

        mockMvc.perform(put("/config/commerce-strategy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strategy\":\"AI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStrategy").value("AI"));

        mockMvc.perform(put("/config/commerce-strategy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strategy\":\"RULE_BASED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStrategy").value("RULE_BASED"));
    }

    private String createProduct() throws Exception {
        String request = """
                {
                  "sku": "SKU-SUGGESTION-API-001",
                  "name": "Suggestion API Lamp",
                  "category": "HOME",
                  "currentPrice": 30.00,
                  "stockLevel": 4,
                  "reorderThreshold": 10,
                  "demandVelocity": 2
                }
                """;

        MvcResult result = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }
}