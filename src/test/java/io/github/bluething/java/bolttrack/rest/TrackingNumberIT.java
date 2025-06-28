package io.github.bluething.java.bolttrack.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class TrackingNumberIT {
    @Container
    static MongoDBAtlasLocalContainer atlasContainer =
            new MongoDBAtlasLocalContainer("mongodb/mongodb-atlas-local:8.0.4");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", atlasContainer::getConnectionString);
        registry.add("spring.data.mongodb.database", () -> "appdb");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanup() {
        // drop the collection so each test starts fresh
        if (mongoTemplate.collectionExists("tracking_numbers")) {
            mongoTemplate.dropCollection("tracking_numbers");
        }
    }

    @Test
    @DisplayName("GET /next-tracking-number with valid params → 200 + persists document")
    void nextViaGet_validRequest_persistsAndReturns() throws Exception {
        var mvcResult = mockMvc.perform(get("/api/v1/next-tracking-number")
                        .param("origin_country_id",      "MY")
                        .param("destination_country_id", "ID")
                        .param("weight",                 "1.234")
                        .param("created_at",             "2025-06-26T10:00:00+00:00")
                        .param("customer_id",            "de619854-b59b-425e-9db4-943979e1bd49")
                        .param("customer_name",          "RedBox Logistics")
                        .param("customer_slug",          "redbox-logistics")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tracking_number").isString())
                .andExpect(jsonPath("$.created_at").isString())
                .andReturn();

        // parse out the response JSON
        String json = mvcResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        String tn = root.get("tracking_number").asText();
        Instant createdAt = Instant.parse(root.get("created_at").asText());

        // verify exactly one document was persisted, and fields match
        Document doc = mongoTemplate.findOne(
                Query.query(Criteria.where("tracking_number").is(tn)),
                Document.class,
                "tracking_numbers"
        );
        assertThat(doc).isNotNull();
        assertThat(doc.getString("tracking_number")).isEqualTo(tn);
        // the DB save records the same generatedAt instant
        Instant dbCreatedAt = doc.getDate("generated_at")
                .toInstant();
        assertThat(dbCreatedAt).isEqualTo(createdAt.truncatedTo(ChronoUnit.MILLIS));
        assertThat(doc.getString("origin_country_id")).isEqualTo("MY");
        assertThat(doc.getString("destination_country_id")).isEqualTo("ID");
        Decimal128 dec = doc.get("weight", Decimal128.class);
        BigDecimal weight = dec.bigDecimalValue();
        assertThat(weight).isEqualByComparingTo(new BigDecimal("1.234"));
        assertThat(doc.getString("customer_name")).isEqualTo("RedBox Logistics");
        assertThat(doc.getString("customer_slug")).isEqualTo("redbox-logistics");
    }

    @Test
    @DisplayName("GET /next-tracking-number with invalid params → 400 + ApiError JSON + no persistence")
    void nextViaGet_invalidRequest_returnsApiErrorAndNoPersist() throws Exception {
        mockMvc.perform(get("/api/v1/next-tracking-number")
                        .param("origin_country_id",      "USA")   // invalid: not 2 uppercase letters
                        .param("destination_country_id", "ID")
                        .param("weight",                 "1.234")
                        .param("created_at",             "bad-timestamp")
                        .param("customer_id",            "not-a-uuid")
                        .param("customer_name",          "")
                        .param("customer_slug",          "Bad_Slug!")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.path").value("/api/v1/next-tracking-number"));

        // ensure nothing was persisted
        long count = mongoTemplate.getCollection("tracking_numbers").countDocuments();
        assertThat(count).isZero();
    }

}
