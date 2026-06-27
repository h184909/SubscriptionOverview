package no.hvl.subscriptionapp.openbanking.lunchflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

public class LunchFlowDtos {

    public record TokenRequest(
            String grant_type,
            String code,
            String redirect_uri,
            String client_id,
            String client_secret,
            String refresh_token
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            String access_token,
            String refresh_token,
            String token_type,
            Long expires_in,
            String user_id
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccountsResponse(
            List<Account> accounts,
            Integer total
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Account(
            String id,
            String name,
            String institution_name,
            String institution_logo,
            String provider,
            String currency,
            String status
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransactionsResponse(
            List<Transaction> transactions,
            Integer total
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transaction(
            String id,
            String accountId,
            BigDecimal amount,
            String currency,
            String date,
            String merchant,
            String description,
            Boolean isPending
    ) {}
}