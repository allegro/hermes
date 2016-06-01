package pl.allegro.tech.hermes.consumers.consumer.sender;

public class MessageSendingResultLogInfo {
    private final String url;
    private final String rootCause;
    private final  Throwable failure;

    public MessageSendingResultLogInfo(String url, Throwable failure, String rootCause) {
        this.url = url;
        this.failure = failure;
        this.rootCause = rootCause;
    }

    public String getUrl() {
        return url;
    }

    public String getRootCause() {
        return rootCause;
    }

    public Throwable getFailure() {
        return failure;
    }
}
