package pl.allegro.tech.hermes.client;

import java.util.function.Predicate;

import static java.net.HttpURLConnection.HTTP_CLIENT_TIMEOUT;

public class HermesClientBasicRetryCondition implements Predicate<HermesResponse> {
    @Override
    public boolean test(HermesResponse response) {
        return isClientTimeoutOrServerError(response) || isFailedExceptionally(response);
    }

    private boolean isClientTimeoutOrServerError(HermesResponse response) {
        return response.getHttpStatus() == HTTP_CLIENT_TIMEOUT || response.getHttpStatus() / 100 == 5;
    }

    private boolean isFailedExceptionally(HermesResponse response) {
        return response.isFailure() && response.getFailureCause().isPresent();
    }
}
