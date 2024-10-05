package pl.allegro.tech.hermes.common.di.factories;

import java.time.Duration;
import java.util.List;

public interface MicrometerRegistryParameters {
  List<Double> getPercentiles();

  boolean zookeeperReporterEnabled();

  Duration zookeeperReportPeriod();
}
