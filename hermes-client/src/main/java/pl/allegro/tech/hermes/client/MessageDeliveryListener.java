package pl.allegro.tech.hermes.client;

public interface MessageDeliveryListener {

    void onSend(HermesResponse response, long latency);

    void onFailure(HermesMessage message, int attemptCount);

    void onFailedRetry(HermesMessage message, int attemptCount);

    void onSuccessfulRetry(HermesMessage message, int attemptCount);

    void onMaxRetriesExceeded(HermesMessage message, int attemptCount);
}
