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
        HermesClient client = hermesClient({uri, msg -> successFuture(msg)})
                .withRetrySleep(0)
                .withMetrics(metrics).build()

        when:
        client.publish("com.group.topic", "123").join()

        then:
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")
        metrics.counter("hermes-client.com_group.topic.status.201").count == 1
    }

    def "should close timer on exceptional completion and log failure metric"() {
        given:
        HermesClient client = hermesClient({uri, msg ->  failingFuture(new RuntimeException())})
                .withRetrySleep(0)
                .withMetrics(metrics).build()

        when:
        silence({ client.publish("com.group.topic", "123").join() })

        then:
        metrics.counter("hermes-client.com_group.topic.failure").count > 0
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")
    }

    def "should update failure metrics and max retries exceeded"() {
        given:
        HermesClient client = hermesClient({uri, msg ->  failingFuture(new RuntimeException())})
                .withRetrySleep(0)
                .withMetrics(metrics).build()

        when:
        silence({ client.publish("com.group.topic", "123").join() })

        then:
        metrics.counter("hermes-client.com_group.topic.failure").count > 0
        metrics.counter("hermes-client.com_group.topic.failure.unsent").count == 1
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")
    }

    def "should update failure metrics with success retry"() {
        given:
        def retries = 3
        HermesClient client = hermesClient(new HermesSender() {
                    int i = 0;
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
                .withMetrics(metrics).build()

        when:
        silence({ client.publish("com.group.topic", "123").join() })

        then:
        metrics.counter("hermes-client.com_group.topic.failure").count > 0
        metrics.counter("hermes-client.com_group.topic.failure.unsent").count == 0
        metrics.counter("hermes-client.com_group.topic.failure.retried").count == 1
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
