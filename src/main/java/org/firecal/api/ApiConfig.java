package org.firecal.api;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configurazione web. CORS: in sviluppo il front-end (Vite) gira su un'altra porta; le origini
 * consentite si cambiano con {@code firecal.cors.allowed-origins} (elenco separato da virgole).
 */
@Configuration
public class ApiConfig implements WebMvcConfigurer {

  private final String[] allowedOrigins;

  public ApiConfig(@Value("${firecal.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String[] allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  /** Lingue dei messaggi dell'API; la prima e' quella di riserva (inglese). */
  static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, Locale.ITALIAN);

  /**
   * Catalogo dei messaggi: {@code messages/api.properties} (inglese, di riserva) e
   * {@code messages/api_it.properties}. Mai la lingua del server: una risposta dipende solo dalla richiesta.
   */
  @Bean
  public MessageSource messageSource() {
    ResourceBundleMessageSource source = new ResourceBundleMessageSource();
    source.setBasename("messages/api");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    source.setAlwaysUseMessageFormat(true);
    return source;
  }

  /** La lingua viene da {@code Accept-Language}; se manca o non e' supportata, inglese. */
  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setSupportedLocales(SUPPORTED_LOCALES);
    resolver.setDefaultLocale(Locale.ENGLISH);
    return resolver;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(allowedOrigins)
        .allowedMethods("GET", "POST", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("ETag", "Content-Disposition")
        .maxAge(3600);
  }
}
