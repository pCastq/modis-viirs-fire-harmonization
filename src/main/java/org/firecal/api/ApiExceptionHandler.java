package org.firecal.api;

import java.net.URI;
import org.firecal.query.ExportTooLargeException;
import org.firecal.query.InvalidAreaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errori in formato Problem Details (RFC 9457): 400 per richieste non valide, 404 per un'area
 * sconosciuta, 413 per un export troppo grande, 503 se mancano i dati. I dettagli interni degli errori
 * del database non escono mai.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private static ProblemDetail problem(HttpStatus status, String title, String detail) {
    ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
    p.setTitle(title);
    p.setType(URI.create("https://firecal.example/problems/" + status.value()));
    return p;
  }

  @ExceptionHandler(InvalidAreaException.class)
  ProblemDetail invalidArea(InvalidAreaException e) {
    return e.isNotFound()
        ? problem(HttpStatus.NOT_FOUND, "Area sconosciuta", e.getMessage())
        : problem(HttpStatus.BAD_REQUEST, "Area non valida", e.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail illegalArgument(IllegalArgumentException e) {
    return problem(HttpStatus.BAD_REQUEST, "Richiesta non valida", e.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail unreadable(HttpMessageNotReadableException e) {
    return problem(HttpStatus.BAD_REQUEST, "Richiesta non leggibile", "Il corpo della richiesta non e' un JSON valido o ha campi del tipo sbagliato");
  }

  @ExceptionHandler(ExportTooLargeException.class)
  ProblemDetail tooLarge(ExportTooLargeException e) {
    return problem(HttpStatus.CONTENT_TOO_LARGE, "Export troppo grande", e.getMessage());
  }

  @ExceptionHandler(DataInfoService.NoDataException.class)
  ProblemDetail noData(DataInfoService.NoDataException e) {
    return problem(HttpStatus.SERVICE_UNAVAILABLE, "Dati non disponibili", e.getMessage());
  }

  @ExceptionHandler({java.sql.SQLException.class, DataAccessException.class})
  ProblemDetail database(Exception e) {
    log.error("Errore del database", e);
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Errore del database", "La richiesta non e' stata completata. Riprovare piu' tardi.");
  }
}
