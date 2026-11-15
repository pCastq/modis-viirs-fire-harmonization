package org.firecal.aggregation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class HarmonizationValidatorTest {

  @Test
  void quantileIsTheValueBelowWhichTheGivenShareOfCasesFalls() {
    List<Double> values = List.of(0.5, 0.1, 0.4, 0.2, 0.3, 0.9, 0.8, 0.7, 0.6, 1.0);
    assertThat(HarmonizationValidator.quantile(values, 0.5)).isEqualTo(0.6);
    assertThat(HarmonizationValidator.quantile(values, 0.8)).isEqualTo(0.9);
    assertThat(HarmonizationValidator.quantile(values, 1.0)).isEqualTo(1.0);
    assertThat(HarmonizationValidator.quantile(values, 0.0)).isEqualTo(0.1);
  }

  @Test
  void quantileDoesNotReorderTheCallersList() {
    List<Double> values = new java.util.ArrayList<>(List.of(3.0, 1.0, 2.0));
    HarmonizationValidator.quantile(values, 0.5);
    assertThat(values).containsExactly(3.0, 1.0, 2.0);
  }

  @Test
  void quantileOfNothingIsNaNNotZero() {
    assertThat(HarmonizationValidator.quantile(List.of(), 0.8)).isNaN();
  }
}
