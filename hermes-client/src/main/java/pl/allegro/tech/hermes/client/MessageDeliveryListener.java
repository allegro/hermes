package pl.allegro.tech.hermes.client;

public interface MessageDeliveryListener {

  void onSend(HermesResponse response, long latency);

  void onFailure(HermesResponse message, int attemptCount);

  void onFailedRetry(HermesResponse message, int attemptCount);

  void onSuccessfulRetry(HermesResponse message, int attemptCount);

  void onMaxRetriesExceeded(HermesResponse message, int attemptCount);
}
