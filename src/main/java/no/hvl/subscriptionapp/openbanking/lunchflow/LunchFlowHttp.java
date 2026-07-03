package no.hvl.subscriptionapp.openbanking.lunchflow;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LunchFlowHttp {

    private final RestClient apiClient;
    private final RestClient oauthClient;
    private final LunchFlowProperties props;

    public LunchFlowHttp(LunchFlowProperties props) {
        this.props = props;

        this.apiClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        this.oauthClient = RestClient.builder()
                .baseUrl(props.getOauthUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        System.out.println("================================");
        System.out.println("LunchFlow baseUrl = " + props.getBaseUrl());
        System.out.println("LunchFlow oauthUrl = " + props.getOauthUrl());
        System.out.println("LunchFlow authorizeUrl = " + props.getAuthorizeUrl());
        System.out.println("LunchFlow redirectUri = " + props.getRedirectUri());
        System.out.println("================================");
    }

    public LunchFlowDtos.TokenResponse exchangeCode(LunchFlowDtos.TokenRequest body) {
        System.out.println("LunchFlow POST " + props.getOauthUrl() + "/oauth/token");

        return oauthClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(LunchFlowDtos.TokenResponse.class);
    }

    public LunchFlowDtos.AccountsResponse getAccounts(String accessToken) {
        System.out.println("LunchFlow GET " + props.getBaseUrl() + "/accounts");

        return apiClient.get()
                .uri("/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(LunchFlowDtos.AccountsResponse.class);
    }

    public LunchFlowDtos.TransactionsResponse getTransactions(
            String accessToken,
            String accountId,
            String fromDate
    ) {
        return apiClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/accounts/{accountId}/transactions")
                            .queryParam("include_pending", "true");

                    if (fromDate != null && !fromDate.isBlank()) {
                        builder.queryParam("from", fromDate);
                    }

                    return builder.build(accountId);
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(LunchFlowDtos.TransactionsResponse.class);
    }
}