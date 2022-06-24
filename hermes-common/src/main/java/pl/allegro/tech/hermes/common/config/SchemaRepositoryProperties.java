package pl.allegro.tech.hermes.common.config;

public class SchemaRepositoryProperties {

    private String serverUrl = "http://localhost:8888/";

    private int httpReadTimeoutMs = 2000;

    private int httpConnectTimeoutMs = 2000;

    private double onlineCheckPermitsPerSecond = 100.0;

    private int onlineCheckAcquireWaitMs = 500;

    private boolean subjectSuffixEnabled = false;

    private boolean subjectNamespaceEnabled = false;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public int getHttpReadTimeoutMs() {
        return httpReadTimeoutMs;
    }

    public void setHttpReadTimeoutMs(int httpReadTimeoutMs) {
        this.httpReadTimeoutMs = httpReadTimeoutMs;
    }

    public int getHttpConnectTimeoutMs() {
        return httpConnectTimeoutMs;
    }

    public void setHttpConnectTimeoutMs(int httpConnectTimeoutMs) {
        this.httpConnectTimeoutMs = httpConnectTimeoutMs;
    }

    public double getOnlineCheckPermitsPerSecond() {
        return onlineCheckPermitsPerSecond;
    }

    public void setOnlineCheckPermitsPerSecond(double onlineCheckPermitsPerSecond) {
        this.onlineCheckPermitsPerSecond = onlineCheckPermitsPerSecond;
    }

    public int getOnlineCheckAcquireWaitMs() {
        return onlineCheckAcquireWaitMs;
    }

    public void setOnlineCheckAcquireWaitMs(int onlineCheckAcquireWaitMs) {
        this.onlineCheckAcquireWaitMs = onlineCheckAcquireWaitMs;
    }

    public boolean isSubjectSuffixEnabled() {
        return subjectSuffixEnabled;
    }

    public void setSubjectSuffixEnabled(boolean subjectSuffixEnabled) {
        this.subjectSuffixEnabled = subjectSuffixEnabled;
    }

    public boolean isSubjectNamespaceEnabled() {
        return subjectNamespaceEnabled;
    }

    public void setSubjectNamespaceEnabled(boolean subjectNamespaceEnabled) {
        this.subjectNamespaceEnabled = subjectNamespaceEnabled;
    }
}
