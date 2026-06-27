package no.hvl.subscriptionapp.openbanking.lunchflow;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LunchFlowHttp {

    private final RestClient client;
    private final LunchFlowProperties props;

    public LunchFlowHttp(LunchFlowProperties props) {
        this.props = props;
        this.client = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        System.out.println("================================");
        System.out.println("LunchFlow baseUrl = " + props.getBaseUrl());
        System.out.println("LunchFlow authorizeUrl = " + props.getAuthorizeUrl());
        System.out.println("LunchFlow redirectUri = " + props.getRedirectUri());
        System.out.println("================================");
    }

    public LunchFlowDtos.TokenResponse exchangeCode(LunchFlowDtos.TokenRequest body) {
        System.out.println("LunchFlow POST " + props.getBaseUrl() + "/oauth/token");

        return client.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(LunchFlowDtos.TokenResponse.class);
    }

    public LunchFlowDtos.AccountsResponse getAccounts(String accessToken) {
        System.out.println("LunchFlow GET " + props.getBaseUrl() + "/accounts");

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
        System.out.println("LunchFlow GET " + props.getBaseUrl() + "/accounts/" + accountId + "/transactions");

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