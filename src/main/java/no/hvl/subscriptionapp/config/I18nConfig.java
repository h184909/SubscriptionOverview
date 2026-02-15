package no.hvl.subscriptionapp.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * messages_en.properties / messages_nb.properties (UTF-8)
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        ms.setUseCodeAsDefaultMessage(true);
        return ms;
    }

    /**
     * Default: EN, men hvis nettleser (Accept-Language) sier NO/nb -> norsk.
     * Bruker kan alltid overstyre via ?lang=en eller ?lang=no (lagres i session).
     */
    @Bean
    public LocaleResolver localeResolver() {
        return new SmartSessionLocaleResolver();
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor i = new LocaleChangeInterceptor();
        i.setParamName("lang"); // ?lang=en eller ?lang=no
        return i;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    /**
     * Custom resolver:
     * 1) Hvis session allerede har locale -> bruk den
     * 2) Ellers: se Accept-Language og velg nb_NO hvis norsk er foretrukket
     * 3) Ellers: en_US
     */
    private static class SmartSessionLocaleResolver extends SessionLocaleResolver {

        private static final Locale EN = Locale.forLanguageTag("en-US");
        private static final Locale NO = Locale.forLanguageTag("nb-NO");

        private static final List<Locale> SUPPORTED = List.of(NO, EN);

        @Override
        public Locale resolveLocale(HttpServletRequest request) {
            // 1) hvis bruker har valgt språk før
            Locale sessionLocale = (Locale) request.getSession().getAttribute(LOCALE_SESSION_ATTRIBUTE_NAME);
            if (sessionLocale != null) return sessionLocale;

            // 2) ellers: prøv å velge basert på Accept-Language
            String header = request.getHeader("Accept-Language");
            if (header != null && !header.isBlank()) {
                try {
                    List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(header);
                    Locale match = Locale.lookup(ranges, SUPPORTED);
                    if (match != null) return match;
                } catch (Exception ignored) {
                }
            }

            // 3) fallback: engelsk som default
            return EN;
        }
    }
}