package pl.allegro.tech.hermes.client

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import pl.allegro.tech.hermes.client.metrics.MetricsProvider
import pl.allegro.tech.hermes.client.metrics.MicrometerTaggedMetricsProvider
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

import static java.util.concurrent.CompletableFuture.completedFuture
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient

class HermesClientMicrometerTaggedMetricsTest extends Specification {

    private MeterRegistry metrics = new SimpleMeterRegistry()
    private MetricsProvider metricsProvider = new MicrometerTaggedMetricsProvider(metrics)

    def "should measure publish latency"() {
        given:
        HermesClient client = hermesClient(delayedHermesSender(Duration.ofMillis(100)))
                .withRetrySleep(0)
                .withMetrics(metricsProvider).build()

        when:
        client.publish("com.group.topic", "123").join()

        then:
        metrics.counter("hermes-client.status", "code", String.valueOf(201), "topic", "com_group.topic").count() == 1
        def timer = metrics.timer("hermes-client.latency", "topic", "com_group.topic")
        timer.totalTime(TimeUnit.NANOSECONDS) >= Duration.ofMillis(100).toNanos()
        timer.totalTime(TimeUnit.NANOSECONDS) < Duration.ofMillis(1000).toNanos()
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
        metrics.counter("hermes-client.failure", "topic", "com_group.topic").count()  == 4
        metrics.timer("hermes-client.latency", "topic", "com_group.topic").count() == 4
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
        metrics.counter("hermes-client.failure", "topic", "com_group.topic").count() == 4
        metrics.counter("hermes-client.retries.count", "topic", "com_group.topic").count() == 3
        metrics.counter("hermes-client.retries.exhausted", "topic", "com_group.topic").count() == 1
        metrics.counter("hermes-client.retries.success", "topic", "com_group.topic").count() == 0
        metrics.summary("hermes-client.retries.attempts", "topic", "com_group.topic").takeSnapshot().count() == 0
        metrics.timer("hermes-client.latency", "topic", "com_group.topic").count() == 4
    }

    def "should update retries metrics"() {
        def retries = 3
        HermesClient client = hermesClient(failingHermesSender(retries - 1))
                .withRetrySleep(0)
                .withRetries(retries)
                .withMetrics(metricsProvider).build()

        when:
        silence({ client.publish("com.group.topic", "123").join() })

        then:
        metrics.counter("hermes-client.failure", "topic", "com_group.topic").count() == 2
        metrics.counter("hermes-client.retries.exhausted", "topic", "com_group.topic").count() == 0
        metrics.counter("hermes-client.retries.success", "topic", "com_group.topic").count() == 1
        metrics.counter("hermes-client.status", "code", String.valueOf(201), "topic", "com_group.topic").count() == 1
        metrics.counter("hermes-client.retries.count", "topic", "com_group.topic").count() == 2
        metrics.summary("hermes-client.retries.attempts", "topic", "com_group.topic").takeSnapshot().count() == 1
        metrics.timer("hermes-client.latency", "topic", "com_group.topic").count() == 3
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
