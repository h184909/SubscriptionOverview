package no.hvl.subscriptionapp.openbanking;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class YapilyHttp {

    private final RestClient client;
    private final ObjectMapper om;

    public YapilyHttp(YapilyProperties props, ObjectMapper om) {
        this.om = om;

        String uuid = props.getApplicationUuid();     // = yapily.app-id
        String secret = props.getApplicationSecret(); // = yapily.secret

        if (uuid == null || uuid.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Mangler Yapily credentials. Sett yapily.app-id og yapily.secret (evt env vars)."
            );
        }

        String basic = Base64.getEncoder()
                .encodeToString((uuid + ":" + secret).getBytes(StandardCharsets.UTF_8));

        this.client = RestClient.builder()
                .baseUrl(props.getBaseUrl()) // = yapily.base-url
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public <T> T get(String path, HttpHeaders extraHeaders, TypeReference<T> type) {
        String json = client.get()
                .uri(path)
                .headers(h -> {
                    if (extraHeaders != null) h.addAll(extraHeaders);
                })
                .retrieve()
                .body(String.class);

        try {
            return om.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Kunne ikke parse Yapily JSON", e);
        }
    }

    public <T> T postJson(String path, Object body, TypeReference<T> type) {
        String json = client.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            return om.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Kunne ikke parse Yapily JSON", e);
        }
    }

    public static HttpHeaders consentHeader(String consentToken) {
        HttpHeaders h = new HttpHeaders();
        h.add("Consent", consentToken);
        return h;
    }
}
