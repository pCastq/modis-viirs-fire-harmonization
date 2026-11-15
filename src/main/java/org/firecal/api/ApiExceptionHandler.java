package org.firecal.api;

import java.net.URI;
import org.firecal.query.ExportTooLargeException;
import org.firecal.query.InvalidAreaException;
import org.firecal.query.Localizable;
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

  private final ApiMessages messages;

  public ApiExceptionHandler(ApiMessages messages) {
    this.messages = messages;
  }

  private ProblemDetail problem(HttpStatus status, String titleCode, String detail) {
    ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
    p.setTitle(messages.text(titleCode));
    p.setType(URI.create("https://firecal.example/problems/" + status.value()));
    return p;
  }

  @ExceptionHandler(InvalidAreaException.class)
  ProblemDetail invalidArea(InvalidAreaException e) {
    return e.isNotFound()
        ? problem(HttpStatus.NOT_FOUND, "error.area.unknown", messages.text(e))
        : problem(HttpStatus.BAD_REQUEST, "error.area.invalid", messages.text(e));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail illegalArgument(IllegalArgumentException e) {
    String detail = e instanceof Localizable localizable ? messages.text(localizable) : e.getMessage();
    return problem(HttpStatus.BAD_REQUEST, "error.request.invalid", detail);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail unreadable(HttpMessageNotReadableException e) {
    return problem(HttpStatus.BAD_REQUEST, "error.request.unreadable", messages.text("error.request.unreadable.detail"));
  }

  @ExceptionHandler(ExportTooLargeException.class)
  ProblemDetail tooLarge(ExportTooLargeException e) {
    return problem(HttpStatus.CONTENT_TOO_LARGE, "error.export.tooLarge", messages.text(e));
  }

  @ExceptionHandler(DataInfoService.NoDataException.class)
  ProblemDetail noData(DataInfoService.NoDataException e) {
    return problem(HttpStatus.SERVICE_UNAVAILABLE, "error.data.unavailable", messages.text(e));
  }

  @ExceptionHandler({java.sql.SQLException.class, DataAccessException.class})
  ProblemDetail database(Exception e) {
    log.error("Database error", e);
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "error.database", messages.text("error.database.detail"));
  }
}
