package no.hvl.subscriptionapp.openbanking.lunchflow;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lunchflow")
public class LunchFlowProperties {

    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private String redirectUri;

    public String getBaseUrl() { return baseUrl; }
    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getRedirectUri() { return redirectUri; }

    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
}