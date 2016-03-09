package pl.allegro.tech.hermes.client

import com.codahale.metrics.MetricRegistry
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static java.util.concurrent.CompletableFuture.completedFuture
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient

class HermesClientMetricsTest extends Specification {

    private final MetricRegistry metrics = new MetricRegistry()

    def "should register latency timer in sanitized path"() {
        given:
        HermesClient client = hermesClient({uri, msg -> completedFuture({201} as HermesResponse)}).withMetrics(metrics).build()

        when:
        client.publish("com.group.topic", "123").join()

        then:
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")
        metrics.counter("hermes-client.com_group.topic.status.201").count == 1
    }

    def "should close timer on exceptional completion and log failure metric"() {
        given:
        HermesClient client = hermesClient({uri, msg ->  failingFuture(new RuntimeException())}).withMetrics(metrics).build()

        when:
        silence({ client.publish("com.group.topic", "123").join() })

        then:
        metrics.counter("hermes-client.com_group.topic.failure").count > 0
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")
    }

    private CompletableFuture<HermesResponse> failingFuture(Throwable throwable) {
        CompletableFuture<HermesResponse> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    private void silence(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception ex) {
            // do nothing
        }
    }

}
