package pl.allegro.tech.hermes.integrationtests.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrometheusMetricsAssertion {

  private static final Pattern METRIC_LINE_PATTERN =
      Pattern.compile("^[a-z_]+\\{(.*)\\} (\\d+\\.\\d+)$");

  private final String actualBody;

  PrometheusMetricsAssertion(String actualBody) {
    this.actualBody = actualBody;
  }

  public PrometheusMetricWithNameAssertion contains(String metricName) {
    List<String> matchedLines =
        actualBody.lines().filter(line -> line.startsWith(metricName + "{")).toList();
    assertThat(matchedLines).overridingErrorMessage("Metric %s doesn't exist").isNotEmpty();
    return new PrometheusMetricWithNameAssertion(matchedLines);
  }

  public static class PrometheusMetricWithNameAssertion {

    private final List<String> actualMetrics;

    PrometheusMetricWithNameAssertion(List<String> actualMetrics) {
      this.actualMetrics = actualMetrics;
    }

    public PrometheusMetricAssertion withLabels(
        String label0, String value0, String label1, String value1) {
      return withLabels(new String[] {label0, label1}, new String[] {value0, value1});
    }

    public PrometheusMetricAssertion withLabels(
        String label0, String value0, String label1, String value1, String label2, String value2) {
      return withLabels(
          new String[] {label0, label1, label2}, new String[] {value0, value1, value2});
    }

    public PrometheusMetricAssertion withLabels(
        String label0,
        String value0,
        String label1,
        String value1,
        String label2,
        String value2,
        String label3,
        String value3) {
      return withLabels(
          new String[] {label0, label1, label2, label3},
          new String[] {value0, value1, value2, value3});
    }

    private PrometheusMetricAssertion withLabels(String[] names, String[] values) {
      List<String> matchedLines =
          actualMetrics.stream()
              .filter(
                  line -> {
                    Matcher matcher = METRIC_LINE_PATTERN.matcher(line);
                    String labels;
                    if (matcher.matches()) {
                      labels = matcher.group(1);
                    } else {
                      throw new IllegalStateException("Unexpected line: " + line);
                    }
                    for (int i = 0; i < names.length; i++) {
                      if (!labels.contains(names[i] + "=\"" + values[i] + "\"")) {
                        return false;
                      }
                    }
                    return true;
                  })
              .toList();
      assertThat(matchedLines)
          .overridingErrorMessage("There is no metric with provided labels")
          .isNotEmpty();
      assertThat(matchedLines)
          .overridingErrorMessage("Found more than one metric with provided labels")
          .hasSize(1);
      return new PrometheusMetricAssertion(matchedLines.get(0));
    }
  }

  public static class PrometheusMetricAssertion {

    private final String actualLine;

    PrometheusMetricAssertion(String actualLine) {
      this.actualLine = actualLine;
    }

    public void withValue(double expectedValue) {
      double actualValue = extractValue();
      assertThat(actualValue).isEqualTo(expectedValue);
    }

    public double withInitialValue() {
      return extractValue();
    }

    public void withValueGreaterThan(double expectedValue) {
      double actualValue = extractValue();
      assertThat(actualValue).isGreaterThan(expectedValue);
    }

    private double extractValue() {
      Matcher matcher = METRIC_LINE_PATTERN.matcher(actualLine);
      if (matcher.matches()) {
        String valueStr = matcher.group(2);
        return Double.parseDouble(valueStr);
      } else {
        throw new IllegalStateException("Unexpected line: " + actualLine);
      }
    }
  }
}
