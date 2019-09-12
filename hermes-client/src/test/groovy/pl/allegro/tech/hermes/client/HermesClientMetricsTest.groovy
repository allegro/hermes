package pl.allegro.tech.hermes.client

import com.codahale.metrics.MetricRegistry
import pl.allegro.tech.hermes.client.metrics.DropwizardMetricsProvider
import pl.allegro.tech.hermes.client.metrics.MetricsProvider
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static java.util.concurrent.CompletableFuture.completedFuture
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient

class HermesClientMetricsTest extends Specification {

    private final MetricRegistry metrics = new MetricRegistry();
    private final MetricsProvider metricsProvider = new DropwizardMetricsProvider(metrics)

    def "should register latency timer in sanitized path"() {
        given:
        HermesClient client = hermesClient({uri, msg -> successFuture(msg)})
                .withRetrySleep(0)
                .withMetrics(metricsProvider).build()

        when:
        client.publish("com.group.topic", "123").join()

        then:
        metrics.counter("hermes-client.com_group.topic.status.201").count == 1
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")
    }

    def "should close timer on exceptional completion and log failure metric"() {
        given:
        HermesClient client = hermesClient({uri, msg ->  failingFuture(new RuntimeException())})
                .withRetrySleep(0)
                .withRetries(3)
                .withMetrics(metricsProvider).build()

        when:
        silence({ client.publish("com.group.topic", "123").join() })

        then:
        metrics.counter("hermes-client.com_group.topic.failure").count == 4
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")
    }

    def "should update max retries exceeded metric"() {
        given:
        HermesClient client = hermesClient({uri, msg ->  failingFuture(new RuntimeException())})
                .withRetrySleep(0)
                .withRetries(3)
                .withMetrics(metricsProvider).build()

        when:
        silence({ client.publish("com.group.topic", "123").join() })

        then:
        metrics.counter("hermes-client.com_group.topic.failure").count == 4
        metrics.counter("hermes-client.com_group.topic.retries.count").count == 3
        metrics.counter("hermes-client.com_group.topic.retries.exhausted").count == 1
        metrics.counter("hermes-client.com_group.topic.retries.success").count == 0
        metrics.histogram("hermes-client.com_group.topic.retries.attempts").getSnapshot().size() == 0
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")
    }

    def "should update retries metrics"() {
        given:
        def retries = 3
        HermesClient client = hermesClient(new HermesSender() {
                    int i = 0
                    @Override
                    CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
                        i++
                        if(i < retries) {
                            return failingFuture(new RuntimeException())
                        }
                        return successFuture(message)
                    }
                })
                .withRetrySleep(0)
                .withRetries(retries)
                .withMetrics(metricsProvider).build()

        when:
        silence({ client.publish("com.group.topic", "123").join() })

        then:
        metrics.counter("hermes-client.com_group.topic.failure").count == 2
        metrics.counter("hermes-client.com_group.topic.retries.exhausted").count == 0
        metrics.counter("hermes-client.com_group.topic.retries.success").count == 1
        metrics.counter("hermes-client.com_group.topic.retries.count").count == 2
        metrics.histogram("hermes-client.com_group.topic.retries.attempts").getSnapshot().getMin() == 2
        metrics.histogram("hermes-client.com_group.topic.retries.attempts").getSnapshot().getMax() == 2
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")
    }

    private CompletableFuture<HermesResponse> successFuture(HermesMessage message) {
        return completedFuture(HermesResponseBuilder.hermesResponse(message).withHttpStatus(201).build())
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
