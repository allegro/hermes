package pl.allegro.tech.hermes.frontend.buffer;

public class BackupMessagesLoaderParameters {

    private final int maxAgeHours;

    private final int maxResendRetries;

    private final int loadingPauseBetweenResend;

    private final int loadingWaitForBrokerTopicInfo;

    public int getMaxAgeHours() {
        return maxAgeHours;
    }

    public int getMaxResendRetries() {
        return maxResendRetries;
    }

    public int getLoadingPauseBetweenResend() {
        return loadingPauseBetweenResend;
    }

    public int getLoadingWaitForBrokerTopicInfo() {
        return loadingWaitForBrokerTopicInfo;
    }

    public BackupMessagesLoaderParameters(int maxAgeHours,
                                          int maxResendRetries,
                                          int loadingPauseBetweenResend,
                                          int loadingWaitForBrokerTopicInfo) {
        this.maxAgeHours = maxAgeHours;
        this.maxResendRetries = maxResendRetries;
        this.loadingPauseBetweenResend = loadingPauseBetweenResend;
        this.loadingWaitForBrokerTopicInfo = loadingWaitForBrokerTopicInfo;
    }
}
