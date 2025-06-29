package io.github.bluething.java.bolttrack.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bluething.java.bolttrack.persistence.TrackingNumberDocument;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    private TrackingNumberDocument existing;

    @BeforeEach
    void cleanup() {
        // drop the collection so each test starts fresh
        if (mongoTemplate.collectionExists("tracking_numbers")) {
            mongoTemplate.dropCollection("tracking_numbers");
        }
    }
    private void addDummy() {
        existing = new TrackingNumberDocument(
                null,
                "DEF123XYZ",
                "MY",
                "ID",
                new BigDecimal("1.234"),
                UUID.fromString("de619854-b59b-425e-9db4-943979e1bd49"),
                "RedBox Logistics",
                "redbox-logistics",
                Instant.parse("2025-06-26T10:05:00Z"),  // generatedAt
                "CREATED",
                Map.of("fragile", true)
        );
        existing = mongoTemplate.insert(existing);
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
                        .accept(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
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
                        .accept(APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.path").value("/api/v1/next-tracking-number"));

        // ensure nothing was persisted
        long count = mongoTemplate.getCollection("tracking_numbers").countDocuments();
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("GET /track/{tracking_number} with valid existing record → 200 + detail JSON")
    void detail_validTrackingNumber_returnsDetail() throws Exception {
        // arrange: create & insert a document
        TrackingNumberDocument doc = new TrackingNumberDocument (
                null,
                "ABC123XYZ",
                "MY",
                "ID",
                new BigDecimal("1.234"),
                UUID.fromString("de619854-b59b-425e-9db4-943979e1bd49"),
                "RedBox Logistics",
                "redbox-logistics",
                Instant.parse("2025-06-26T10:00:00Z"),
                "CREATED",
                Map.of("fragile", true)
        );
        mongoTemplate.insert(doc);

        // act & assert
        var mvc = mockMvc.perform(get("/api/v1/track/{tn}", doc.getTrackingNumber())
                        .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.tracking_number").value(doc.getTrackingNumber()))
                .andExpect(jsonPath("$.origin_country_id").value("MY"))
                .andExpect(jsonPath("$.destination_country_id").value("ID"))
                .andExpect(jsonPath("$.weight").value(1.234))
                .andExpect(jsonPath("$.generated_at").value("2025-06-26T10:00:00Z"))
                .andExpect(jsonPath("$.customer_id").value(doc.getCustomerId().toString()))
                .andExpect(jsonPath("$.customer_name").value("RedBox Logistics"))
                .andExpect(jsonPath("$.customer_slug").value("redbox-logistics"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.metadata.fragile").value(true))
                .andReturn();

        // verify persistence via raw MongoTemplate
        Document saved = mongoTemplate.findOne(
                org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("tracking_number")
                                .is(doc.getTrackingNumber())
                ), Document.class, "tracking_numbers");
        assertThat(saved).isNotNull();
        assertThat(saved.getString("status")).isEqualTo("CREATED");
    }

    @Test
    @DisplayName("GET /track/{tracking_number} with non-existent record → 404 Not Found")
    void detail_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/track/{tn}", "NONEXISTENT123")
                        .accept("application/json"))
                .andExpect(status().isNotFound());
        // ensure collection is still empty
        assertThat(mongoTemplate.getCollection("tracking_numbers").countDocuments()).isZero();
    }

    @Test
    @DisplayName("GET /track/{tracking_number} with invalid ID format → 400 Bad Request")
    void detail_invalidFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/track/{tn}", "invalid_slug!")
                        .accept("application/json"))
                .andExpect(status().isBadRequest());
        // no documents should be created
        assertThat(mongoTemplate.getCollection("tracking_numbers").countDocuments()).isZero();
    }

    @Test
    @DisplayName("PATCH /track/{tracking_number}/status valid transition → 200 + updated status")
    void updateStatus_validTransition_persistsAndReturns() throws Exception {
        addDummy();
        String trackingNumber = existing.getTrackingNumber();
        String payload = objectMapper.writeValueAsString(Map.of("status", "PICKED_UP"));

        var result = mockMvc.perform(patch("/api/v1/track/{trackingNumber}/status", trackingNumber)
                        .contentType(APPLICATION_JSON)
                        .content(payload)
                        .accept(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.tracking_number").value(trackingNumber))
                .andExpect(jsonPath("$.status").value("PICKED_UP"))
                .andReturn();

        // verify in database: status updated
        Document doc = mongoTemplate.findOne(
                org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("tracking_number").is(trackingNumber)
                ),
                Document.class,
                "tracking_numbers"
        );
        assertThat(doc).isNotNull();
        assertThat(doc.getString("status")).isEqualTo("PICKED_UP");
    }

    @Test
    @DisplayName("PATCH /track/{tracking_number}/status invalid transition → 400 Bad Request")
    void updateStatus_invalidTransition_noPersistAndReturnsError() throws Exception {
        addDummy();
        String trackingNumber = existing.getTrackingNumber();
        // Attempt CREATED -> DELIVERED which is not allowed
        String payload = objectMapper.writeValueAsString(Map.of("status", "DELIVERED"));

        mockMvc.perform(patch("/api/v1/track/{trackingNumber}/status", trackingNumber)
                        .contentType(APPLICATION_JSON)
                        .content(payload)
                        .accept(APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot transition status from CREATED to DELIVERED"));

        // verify in database: status remains CREATED
        Document doc = mongoTemplate.findOne(
                org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("tracking_number").is(trackingNumber)
                ),
                Document.class,
                "tracking_numbers"
        );
        assertThat(doc.getString("status")).isEqualTo("CREATED");
    }

    @Test
    @DisplayName("PATCH /track/{tracking_number}/status missing status field → 400 Validation Error")
    void updateStatus_missingStatus_returnsValidationError() throws Exception {
        addDummy();
        String trackingNumber = existing.getTrackingNumber();
        // empty JSON body
        mockMvc.perform(patch("/api/v1/track/{tn}/status", trackingNumber)
                        .contentType(APPLICATION_JSON)
                        .content("{}")
                        .accept(APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field", containsString("status")))
                .andExpect(jsonPath("$.path").value("/api/v1/track/" + trackingNumber + "/status"));
    }

    @Test
    @DisplayName("PATCH /track/{tracking_number}/status non-existent → 404 Not Found")
    void updateStatus_nonExistent_returns404() throws Exception {
        // drop collection to ensure non-existence
        mongoTemplate.dropCollection("tracking_numbers");

        String payload = objectMapper.writeValueAsString(Map.of("status", "PICKED_UP"));
        mockMvc.perform(patch("/api/v1/track/{tn}/status", "DOESNOTEXIST")
                        .contentType(APPLICATION_JSON)
                        .content(payload)
                        .accept(APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Resource TrackingNumber not found with identifier")))
                .andExpect(jsonPath("$.path").value("/api/v1/track/DOESNOTEXIST/status"));
    }

}
