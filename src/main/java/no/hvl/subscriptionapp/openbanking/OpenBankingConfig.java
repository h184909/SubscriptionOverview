package no.hvl.subscriptionapp.openbanking;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(YapilyProperties.class)
public class OpenBankingConfig {
}
