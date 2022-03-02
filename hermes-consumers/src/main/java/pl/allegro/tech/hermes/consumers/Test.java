package pl.allegro.tech.hermes.consumers;

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.pubsub.v1.Publisher;
import org.threeten.bp.Duration;

import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        final RetrySettings retry = RetrySettings.newBuilder()
                .setTotalTimeout(Duration.ofMillis(10001))
                .setInitialRpcTimeout(Duration.ofSeconds(10))
                .setMaxRpcTimeout(Duration.ofMillis(10001))
                .build();

        final Publisher publisher = Publisher.newBuilder("projects/sc-9620-datahub-staging-prod/topics/hermes-in-pubsub")
                .setRetrySettings(retry)
                .build();
        publisher.shutdown();
    }
}
