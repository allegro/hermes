package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.di.factories.MetricRegistryParameters;

@ConfigurationProperties(prefix = "frontend.metrics")
public class MetricsProperties {

    private boolean zookeeperReporterEnabled = true;

    private boolean graphiteReporterEnabled = false;

    private boolean consoleReporterEnabled = false;

    private int counterExpireAfterAccess = 72;

    private String reservoirType = "exponentially_decaying";

    private String disabledAttributes = "M15_RATE, M5_RATE, MEAN, MEAN_RATE, MIN, STDDEV";

    private int reportPeriod = 20;

    public boolean isZookeeperReporterEnabled() {
        return zookeeperReporterEnabled;
    }

    public void setZookeeperReporterEnabled(boolean zookeeperReporterEnabled) {
        this.zookeeperReporterEnabled = zookeeperReporterEnabled;
    }

    public boolean isGraphiteReporterEnabled() {
        return graphiteReporterEnabled;
    }

    public void setGraphiteReporterEnabled(boolean graphiteReporterEnabled) {
        this.graphiteReporterEnabled = graphiteReporterEnabled;
    }

    public boolean isConsoleReporterEnabled() {
        return consoleReporterEnabled;
    }

    public void setConsoleReporterEnabled(boolean consoleReporterEnabled) {
        this.consoleReporterEnabled = consoleReporterEnabled;
    }

    public int getCounterExpireAfterAccess() {
        return counterExpireAfterAccess;
    }

    public void setCounterExpireAfterAccess(int counterExpireAfterAccess) {
        this.counterExpireAfterAccess = counterExpireAfterAccess;
    }

    public String getReservoirType() {
        return reservoirType;
    }

    public void setReservoirType(String reservoirType) {
        this.reservoirType = reservoirType;
    }

    public String getDisabledAttributes() {
        return disabledAttributes;
    }

    public void setDisabledAttributes(String disabledAttributes) {
        this.disabledAttributes = disabledAttributes;
    }

    public int getReportPeriod() {
        return reportPeriod;
    }

    public void setReportPeriod(int reportPeriod) {
        this.reportPeriod = reportPeriod;
    }

    protected MetricRegistryParameters toMetricRegistryParameters() {
        return new MetricRegistryParameters(
                this.zookeeperReporterEnabled,
                this.graphiteReporterEnabled,
                this.consoleReporterEnabled,
                this.reservoirType,
                this.disabledAttributes,
                this.reportPeriod
        );
    }
}
