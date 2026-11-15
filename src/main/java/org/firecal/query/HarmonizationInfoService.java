package org.firecal.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.firecal.domain.CellDayFlags;
import org.firecal.domain.ConfidenceClass;
import org.firecal.domain.DayNightFilter;
import org.firecal.domain.SourceTypeFilter;
import org.firecal.domain.harmonization.CountryKMethod;
import org.firecal.domain.harmonization.KResolution;
import org.firecal.domain.harmonization.MethodRef;
import org.firecal.domain.harmonization.SatelliteGroup;
import org.firecal.domain.harmonization.UncertaintyBands;
import org.springframework.stereotype.Service;

/**
 * Trasparenza sull'armonizzazione: quali moltiplicatori k usa il metodo per un paese (uno per trimestre e per giorno/notte)
 * e con quale incertezza. Sono gli stessi k che applica il calendario, letti dallo stesso metodo: non una copia.
 */
@Service
public class HarmonizationInfoService {

  private static final int QUARTERS = 4;

  /** Il k di un trimestre e di un filtro giorno/notte, con le somme di celle-giorno da cui nasce. */
  public record Factor(
      int quarter,
      DayNightFilter dayNight,
      Optional<Double> k,
      Optional<CountryKMethod.KSource> source,
      long viirsCellDays,
      long modisCellDays) {}

  /** Incertezza dichiarata per una fascia di celle-giorno MODIS del mese. */
  public record Band(String level, double relativeUncertainty) {}

  /** Tutto quello che si mostra sull'armonizzazione di un paese. */
  public record Info(
      MethodRef method,
      Optional<CalibrationService.Info> calibration,
      String country,
      List<Factor> factors,
      List<Band> bands) {}

  private final MethodRegistry registry;
  private final CalibrationService calibration;
  private final AreaResolver resolver;

  public HarmonizationInfoService(MethodRegistry registry, CalibrationService calibration, AreaResolver resolver) {
    this.registry = registry;
    this.calibration = calibration;
    this.resolver = resolver;
  }

  /**
   * @param countryCode un paese del perimetro, come negli altri endpoint
   * @throws InvalidAreaException se il paese non esiste
   */
  public Info describe(String countryCode, SourceTypeFilter sourceType, ConfidenceClass minConfidence) {
    resolver.resolve(AreaSpec.ofCountry(countryCode));
    CountryKMethod method = registry.countryK(KResolution.QUARTER);
    List<Factor> factors = new ArrayList<>();
    for (int quarter = 1; quarter <= QUARTERS; quarter++) {
      for (DayNightFilter dayNight : DayNightFilter.values()) {
        factors.add(factor(method, countryCode, quarter, dayNight, sourceType, minConfidence));
      }
    }
    return new Info(method.ref(), calibration.info(), countryCode, factors, bands());
  }

  private static Factor factor(
      CountryKMethod method, String country, int quarter, DayNightFilter dayNight, SourceTypeFilter sourceType, ConfidenceClass minConfidence) {
    int counter = CellDayFlags.counterIndex(sourceType, dayNight, minConfidence);
    int firstMonthOfQuarter = (quarter - 1) * 3 + 1;
    return method
        .countryFactor(country, firstMonthOfQuarter, SatelliteGroup.TERRA_AQUA, counter)
        .map(applied -> new Factor(
            quarter, dayNight, Optional.of(applied.k()), Optional.of(applied.source()),
            applied.stratum().viirs(), applied.stratum().old()))
        .orElseGet(() -> new Factor(quarter, dayNight, Optional.empty(), Optional.empty(), 0, 0));
  }

  private static List<Band> bands() {
    List<Band> bands = new ArrayList<>();
    for (int level = 0; level < UncertaintyBands.LEVELS; level++) {
      bands.add(new Band(UncertaintyBands.levelLabel(level), UncertaintyBands.countryBand(KResolution.QUARTER, level)));
    }
    return bands;
  }
}
