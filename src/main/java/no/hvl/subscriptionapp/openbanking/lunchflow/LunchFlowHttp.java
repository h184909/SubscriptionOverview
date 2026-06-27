package no.hvl.subscriptionapp.openbanking.lunchflow;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LunchFlowHttp {

    private final RestClient client;

    public LunchFlowHttp(LunchFlowProperties props) {
        this.client = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public LunchFlowDtos.TokenResponse exchangeCode(LunchFlowDtos.TokenRequest body) {
        return client.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(LunchFlowDtos.TokenResponse.class);
    }

    public LunchFlowDtos.AccountsResponse getAccounts(String accessToken) {
        return client.get()
                .uri("/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(LunchFlowDtos.AccountsResponse.class);
    }

    public LunchFlowDtos.TransactionsResponse getTransactions(
            String accessToken,
            String accountId
    ) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/accounts/{accountId}/transactions")
                        .queryParam("include_pending", "true")
                        .build(accountId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(LunchFlowDtos.TransactionsResponse.class);
    }
}