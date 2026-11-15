package org.firecal.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
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
