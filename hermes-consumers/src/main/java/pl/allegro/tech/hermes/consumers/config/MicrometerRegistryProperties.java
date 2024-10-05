package pl.allegro.tech.hermes.consumers.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.di.factories.MicrometerRegistryParameters;

@ConfigurationProperties(prefix = "consumer.metrics.micrometer")
public class MicrometerRegistryProperties implements MicrometerRegistryParameters {

  private List<Double> percentiles = List.of(0.5, 0.99, 0.999);
  private boolean zookeeperReporterEnabled = true;
  private Duration reportPeriod = Duration.ofSeconds(20);

  @Override
  public List<Double> getPercentiles() {
    return percentiles;
  }

  @Override
  public boolean zookeeperReporterEnabled() {
    return zookeeperReporterEnabled;
  }

  @Override
  public Duration zookeeperReportPeriod() {
    return reportPeriod;
  }

  public void setPercentiles(List<Double> percentiles) {
    this.percentiles = percentiles;
  }

  public boolean isZookeeperReporterEnabled() {
    return zookeeperReporterEnabled;
  }

  public void setZookeeperReporterEnabled(boolean zookeeperReporterEnabled) {
    this.zookeeperReporterEnabled = zookeeperReporterEnabled;
  }

  public Duration getReportPeriod() {
    return reportPeriod;
  }

  public void setReportPeriod(Duration reportPeriod) {
    this.reportPeriod = reportPeriod;
  }
}
