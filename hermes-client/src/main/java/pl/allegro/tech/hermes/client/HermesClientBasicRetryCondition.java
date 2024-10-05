package pl.allegro.tech.hermes.client;

import static java.net.HttpURLConnection.HTTP_CLIENT_TIMEOUT;

import java.util.function.Predicate;

public class HermesClientBasicRetryCondition implements Predicate<HermesResponse> {
  @Override
  public boolean test(HermesResponse response) {
    return response == null
        || (isClientTimeoutOrServerError(response) || isFailedExceptionally(response));
  }

  private boolean isClientTimeoutOrServerError(HermesResponse response) {
    return response.getHttpStatus() == HTTP_CLIENT_TIMEOUT || response.getHttpStatus() / 100 == 5;
  }

  private boolean isFailedExceptionally(HermesResponse response) {
    return response.isFailure() && response.getFailureCause().isPresent();
  }
}
