package org.firecal.api;

import java.util.List;
import java.util.Map;

/** Le risposte dell'API v1. Record semplici: la forma e' quella documentata da OpenAPI. */
public final class ApiDtos {

  private ApiDtos() {}

  public record AreaInfo(String label, String kind, double west, double south, double east, double north) {}

  public record MethodInfo(String id, String version) {}

  public record FilterInfo(String sourceType, String dayNight, String minConfidence) {}

  /**
   * Il calendario: matrice anni x 12 mesi.
   *
   * @param values valore armonizzato per anno e mese; {@code null} = nessun dato (non zero)
   * @param provisional mese con dati provvisori (NRT)
   * @param incomplete mese con giorni mancanti: non confrontabile, escluso dalla mediana
   * @param bySatellite conteggi grezzi per satellite (modalita' RAW); {@code null} = non osservato
   * @param typicalUncertainty incertezza relativa tipica del metodo (0,35 = +/-35%), se nota
   * @param notes avvertenze da mostrare all'utente (metodo provvisorio, risoluzione, filtro)
   */
  public record CalendarResponse(
      AreaInfo area,
      String resolution,
      MethodInfo method,
      String mode,
      FilterInfo filters,
      String from,
      String to,
      List<Integer> years,
      List<Integer> months,
      List<List<Double>> values,
      List<List<Boolean>> provisional,
      List<List<Boolean>> incomplete,
      Map<String, List<List<Integer>>> bySatellite,
      Double typicalUncertainty,
      List<String> notes) {}

  /** L'anomalia di un mese; {@code available = false} se manca il dato o un anno di riferimento. */
  public record AnomalyResponse(
      boolean available,
      String reason,
      String month,
      Double value,
      Double median,
      Double difference,
      Double ratio,
      Double percentileRank,
      Double robustZScore,
      Integer referenceCount,
      List<Integer> referenceYears,
      boolean fewReferenceYears,
      boolean valueIncomplete,
      boolean valueProvisional,
      MethodInfo method,
      String resolution,
      List<String> notes) {}

  public record MonthProfileDto(int month, int years, Double median, Double q1, Double q3, Double min, Double max) {}

  public record SeasonalProfileResponse(List<MonthProfileDto> months, Integer peakMonth, MethodInfo method, String resolution, List<String> notes) {}

  public record AnnualTotalDto(int year, double total, boolean provisional, Double relativeUncertainty) {}

  public record TrendDto(double senSlope, double mannKendallS, double zScore, double pValue, int years, boolean fewYears, boolean significantAt5Percent) {}

  public record AnnualTrendResponse(
      List<AnnualTotalDto> totals, TrendDto trend, Integer worstYear, Integer peakMonth, MethodInfo method, String resolution, List<String> notes) {}

  public record DayDto(String date, Map<String, Integer> bySatellite, Map<String, String> status) {}

  /** I giorni di un mese: la somma dei giorni di un satellite e' il suo valore del mese nel calendario. */
  public record MonthDetailResponse(
      AreaInfo area, String month, FilterInfo filters, List<DayDto> days, Map<String, Integer> monthTotalsBySatellite, List<String> notes) {}

  public record SatelliteInfo(String code, String csvLabel, String sensor, String role, String firstDate, String lastDate, boolean loaded) {}

  public record BoundaryInfo(String code, String name, String level, double west, double south, double east, double north) {}

  public record Metadata(
      String firstMonth,
      String lastMonth,
      MethodInfo activeMethod,
      boolean activeMethodIsProvisional,
      List<MethodInfo> availableMethods,
      List<SatelliteInfo> satellites,
      List<BoundaryInfo> boundaries,
      String lastIngest,
      String defaultSourceType,
      Map<String, Object> limits,
      List<String> notes) {}
}
