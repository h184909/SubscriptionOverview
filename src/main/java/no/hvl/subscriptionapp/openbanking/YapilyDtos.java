package no.hvl.subscriptionapp.openbanking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

public class YapilyDtos {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(String tracingId, Integer count) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiSingleResponse<T>(Meta meta, T data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiListResponse<T>(Meta meta, List<T> data) {}

    // --- Create account auth request ---
    public record CreateAccountAuthRequestBody(String applicationUserId, String institutionId, String callback) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccountAuthRequestData(
            String id,
            String applicationUserId,
            String institutionId,
            String status,
            String authorisationUrl,
            String state
    ) {}


    // --- Transactions ---
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transaction(
            String id,
            String date,
            Double amount,
            String currency,
            String description,
            String reference
    ) {}

    // --- Institutions ---
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Institution(String id, String name, String environmentType) {
        // JSP EL trenger getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getEnvironmentType() { return environmentType; }
    }


}
