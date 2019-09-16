package pl.allegro.tech.hermes.client

import com.codahale.metrics.MetricRegistry
import pl.allegro.tech.hermes.client.metrics.DropwizardMetricsProvider
import pl.allegro.tech.hermes.client.metrics.MetricsProvider
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture

import static java.util.concurrent.CompletableFuture.completedFuture
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient

class HermesClientMetricsTest extends Specification {

    private final MetricRegistry metrics = new MetricRegistry()
    private final MetricsProvider metricsProvider = new DropwizardMetricsProvider(metrics)

    def "should measure publish latency"() {
        given:
        HermesClient client = hermesClient(delayedHermesSender(Duration.ofMillis(100)))
                .withRetrySleep(0)
                .withMetrics(metricsProvider).build()

        when:
        client.publish("com.group.topic", "123").join()

        then:
        metrics.counter("hermes-client.com_group.topic.status.201").count == 1
        metrics.timer("hermes-client.com_group.topic.latency").getSnapshot().getMax() >= Duration.ofMillis(100).get(ChronoUnit.NANOS)
        metrics.timer("hermes-client.com_group.topic.latency").getSnapshot().getMax() < Duration.ofMillis(300).get(ChronoUnit.NANOS)
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
        HermesClient client1 = hermesClient(failingHermesSender(2))
                .withRetrySleep(0)
                .withRetries(4)
                .withMetrics(metricsProvider).build()

        HermesClient client2 = hermesClient(failingHermesSender(5))
                .withRetrySleep(0)
                .withRetries(6)
                .withMetrics(metricsProvider).build()

        HermesClient client3 = hermesClient(failingHermesSender(3))
                .withRetrySleep(0)
                .withRetries(2)
                .withMetrics(metricsProvider).build()

        when:
        silence({ client1.publish("com.group.topic", "123").join() })
        silence({ client2.publish("com.group.topic", "456").join() })
        silence({ client3.publish("com.group.topic", "789").join() })

        then:
        metrics.counter("hermes-client.com_group.topic.failure").count == 10
        metrics.counter("hermes-client.com_group.topic.retries.exhausted").count == 1
        metrics.counter("hermes-client.com_group.topic.retries.success").count == 2
        metrics.counter("hermes-client.com_group.topic.retries.count").count == 9
        metrics.histogram("hermes-client.com_group.topic.retries.attempts").getSnapshot().getMin() == 2
        metrics.histogram("hermes-client.com_group.topic.retries.attempts").getSnapshot().getMax() == 5
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")
    }

    private CompletableFuture<HermesResponse> successFuture(HermesMessage message) {
        return completedFuture(HermesResponseBuilder.hermesResponse(message).withHttpStatus(201).build())
    }

    private CompletableFuture<HermesResponse> failingFuture(Throwable throwable) {
        CompletableFuture<HermesResponse> future = new CompletableFuture<>()
        future.completeExceptionally(throwable)
        return future
    }

    private HermesSender failingHermesSender(int errorNo) {
        new HermesSender() {
            int i = 0
            @Override
            CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
                i++
                if (i <= errorNo) {
                    return failingFuture(new RuntimeException())
                }
                return successFuture(message)
            }
        }
    }

    private HermesSender delayedHermesSender(Duration sendLatencyMs) {
        new HermesSender() {
            @Override
            CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
                Thread.sleep(sendLatencyMs.toMillis())
                return successFuture(message)
            }
        }
    }

    private void silence(Runnable runnable) {
        try {
            runnable.run()
        } catch (Exception ex) {
            // do nothing
        }
    }

}
