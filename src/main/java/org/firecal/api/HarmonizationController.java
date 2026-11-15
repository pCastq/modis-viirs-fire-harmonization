package org.firecal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.firecal.api.ApiDtos.HarmonizationResponse;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.SourceTypeFilter;
import org.firecal.query.HarmonizationInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/** Come si armonizzano MODIS e VIIRS in un paese: i moltiplicatori k realmente usati e la loro incertezza. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Armonizzazione", description = "Moltiplicatori k, calibrazione e incertezza del metodo attivo")
public class HarmonizationController {

  private final HarmonizationInfoService info;
  private final RequestMapper mapper;
  private final PayloadBuilder builder;
  private final DataInfoService data;

  public HarmonizationController(HarmonizationInfoService info, RequestMapper mapper, PayloadBuilder builder, DataInfoService data) {
    this.info = info;
    this.mapper = mapper;
    this.builder = builder;
    this.data = data;
  }

  @GetMapping("/harmonization")
  @Operation(
      summary = "Moltiplicatori di armonizzazione di un paese",
      description = "Il k (hotspot VIIRS per ogni hotspot MODIS) per trimestre e giorno/notte, con le somme di celle-giorno da cui nasce, "
          + "la finestra di calibrazione e le fasce di incertezza. Parametri: boundaryCode (obbligatorio), sourceType e minConfidence (predefiniti come nel calendario).")
  public ResponseEntity<HarmonizationResponse> harmonization(
      @RequestParam String boundaryCode,
      @RequestParam(required = false) String sourceType,
      @RequestParam(required = false) String minConfidence,
      WebRequest request) {
    SourceTypeFilter source = mapper.sourceType(sourceType);
    ConfidenceClass confidence = mapper.minConfidence(minConfidence);
    String etag = data.etag("harmonization|" + boundaryCode + "|" + source + "|" + confidence);
    if (request.checkNotModified(etag)) {
      return null;
    }
    HarmonizationResponse body = builder.harmonization(info.describe(boundaryCode.strip(), source, confidence), source, confidence);
    return ResponseEntity.ok().eTag(etag).body(body);
  }
}
