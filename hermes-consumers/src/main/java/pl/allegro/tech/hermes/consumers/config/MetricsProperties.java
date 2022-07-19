package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.di.factories.MetricRegistryParameters;

@ConfigurationProperties(prefix = "consumer.metrics")
public class MetricsProperties implements MetricRegistryParameters {

    private boolean zookeeperReporterEnabled = true;

    private boolean graphiteReporterEnabled = false;

    private boolean consoleReporterEnabled = false;

    private int counterExpireAfterAccess = 72;

    private String reservoirType = "exponentially_decaying";

    private String disabledAttributes = "M15_RATE, M5_RATE, MEAN, MEAN_RATE, MIN, STDDEV";

    private int reportPeriod = 20;

    @Override
    public boolean isZookeeperReporterEnabled() {
        return zookeeperReporterEnabled;
    }

    public void setZookeeperReporterEnabled(boolean zookeeperReporterEnabled) {
        this.zookeeperReporterEnabled = zookeeperReporterEnabled;
    }

    @Override
    public boolean isGraphiteReporterEnabled() {
        return graphiteReporterEnabled;
    }

    public void setGraphiteReporterEnabled(boolean graphiteReporterEnabled) {
        this.graphiteReporterEnabled = graphiteReporterEnabled;
    }

    @Override
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

    @Override
    public String getReservoirType() {
        return reservoirType;
    }

    public void setReservoirType(String reservoirType) {
        this.reservoirType = reservoirType;
    }

    @Override
    public String getDisabledAttributes() {
        return disabledAttributes;
    }

    public void setDisabledAttributes(String disabledAttributes) {
        this.disabledAttributes = disabledAttributes;
    }

    @Override
    public int getReportPeriod() {
        return reportPeriod;
    }

    public void setReportPeriod(int reportPeriod) {
        this.reportPeriod = reportPeriod;
    }
}
