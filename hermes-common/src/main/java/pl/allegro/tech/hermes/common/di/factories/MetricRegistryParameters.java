package pl.allegro.tech.hermes.common.di.factories;

public class MetricRegistryParameters {

    private final boolean zookeeperReporterEnabled;

    private final boolean graphiteReporterEnabled;

    private final boolean consoleReporterEnabled;

    private final String reservoirType;

    private final String disabledAttributes;

    private final int reportPeriod;

    public boolean isZookeeperReporterEnabled() {
        return zookeeperReporterEnabled;
    }

    public boolean isGraphiteReporterEnabled() {
        return graphiteReporterEnabled;
    }

    public boolean isConsoleReporterEnabled() {
        return consoleReporterEnabled;
    }

    public String getReservoirType() {
        return reservoirType;
    }

    public String getDisabledAttributes() {
        return disabledAttributes;
    }

    public int getReportPeriod() {
        return reportPeriod;
    }

    public MetricRegistryParameters(boolean zookeeperReporterEnabled,
                                    boolean graphiteReporterEnabled,
                                    boolean consoleReporterEnabled,
                                    String reservoirType,
                                    String disabledAttributes,
                                    int reportPeriod) {
        this.zookeeperReporterEnabled = zookeeperReporterEnabled;
        this.graphiteReporterEnabled = graphiteReporterEnabled;
        this.consoleReporterEnabled = consoleReporterEnabled;
        this.reservoirType = reservoirType;
        this.disabledAttributes = disabledAttributes;
        this.reportPeriod = reportPeriod;
    }
}
