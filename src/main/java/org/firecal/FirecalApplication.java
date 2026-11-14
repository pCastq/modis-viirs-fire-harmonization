package org.firecal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto di ingresso dell'applicazione (monolite modulare, specifica 6.2).
 *
 * <p>Il pacchetto {@code org.firecal.domain} e' Java puro e non conosce Spring: la dipendenza va
 * solo dall'applicazione verso il dominio, mai il contrario.
 */
@SpringBootApplication
public class FirecalApplication {

  public static void main(String[] args) {
    SpringApplication.run(FirecalApplication.class, args);
  }
}
