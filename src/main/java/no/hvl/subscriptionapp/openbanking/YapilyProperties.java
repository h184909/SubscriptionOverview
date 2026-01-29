package no.hvl.subscriptionapp.openbanking;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "yapily")
public class YapilyProperties {

    /** yapily.app-id */
    private String appId;

    /** yapily.secret */
    private String secret;

    /** yapily.base-url */
    private String baseUrl;

    /** yapily.callback-url */
    private String callbackUrl;

    /** yapily.environment (sandbox|live) */
    private String environment;

    public String getApplicationUuid() { // behold navnet du allerede bruker i YapilyHttp
        return appId;
    }

    public String getApplicationSecret() {
        return secret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setAppId(String appId) { this.appId = appId; }
    public void setSecret(String secret) { this.secret = secret; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    public void setEnvironment(String environment) { this.environment = environment; }
}
