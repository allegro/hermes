package pl.allegro.tech.hermes.common.di.factories;

import java.time.Duration;

public interface MetricRegistryParameters {

    boolean isZookeeperReporterEnabled();

    boolean isGraphiteReporterEnabled();

    boolean isConsoleReporterEnabled();

    String getReservoirType();

    String getDisabledAttributes();

    Duration getReportPeriod();
}
