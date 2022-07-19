package pl.allegro.tech.hermes.common.di.factories;

public interface MetricRegistryParameters {

    boolean isZookeeperReporterEnabled();

    boolean isGraphiteReporterEnabled();

    boolean isConsoleReporterEnabled();

    String getReservoirType();

    String getDisabledAttributes();

    int getReportPeriod();
}
