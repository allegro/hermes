package pl.allegro.tech.hermes.client

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import pl.allegro.tech.hermes.client.metrics.MetricsProvider
import pl.allegro.tech.hermes.client.metrics.MicrometerMetricsProvider
import reactor.core.publisher.Mono
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import static pl.allegro.tech.hermes.client.ReactiveHermesClientBuilder.hermesClient

class ReactiveHermesClientMicrometerMetricsTest extends Specification {

    private final MeterRegistry metrics = new SimpleMeterRegistry()
    private final MetricsProvider metricsProvider = new MicrometerMetricsProvider(metrics)

    def "should measure publish latency"() {
        given:
        ReactiveHermesClient client = hermesClient(delayedHermesSender(Duration.ofMillis(100)))
                .withRetrySleep(0)
                .withMetrics(metricsProvider).build()

        when:
        client.publish("com.group.topic", "123").block()

        then:
        metrics.counter("hermes-client.com_group.topic.status.{code}", "code", String.valueOf(201)).count() == 1
        def timer = metrics.timer("hermes-client.com_group.topic.latency")
        timer.totalTime(TimeUnit.NANOSECONDS) >= Duration.ofMillis(100).get(ChronoUnit.NANOS)
        timer.totalTime(TimeUnit.NANOSECONDS) < Duration.ofMillis(300).get(ChronoUnit.NANOS)
    }

    def "should close timer on exceptional completion and log failure metric"() {
        given:
        ReactiveHermesClient client = hermesClient({uri, msg ->  failingMono(new RuntimeException())})
                .withRetrySleep(0)
                .withRetries(3)
                .withMetrics(metricsProvider).build()

        when:
        silence({ client.publish("com.group.topic", "123").block() })

        then:
        metrics.counter("hermes-client.com_group.topic.failure").count()  == 4
        metrics.timer("hermes-client.com_group.topic.latency").count() == 4
    }

    def "should update max retries exceeded metric"() {
        given:
        ReactiveHermesClient client = hermesClient({uri, msg ->  failingMono(new RuntimeException())})
                .withRetrySleep(0)
                .withRetries(3)
                .withMetrics(metricsProvider).build()

        when:
        silence({ client.publish("com.group.topic", "123").block() })

        then:
        metrics.counter("hermes-client.com_group.topic.failure").count() == 4
        metrics.counter("hermes-client.com_group.topic.retries.count").count() == 3
        metrics.counter("hermes-client.com_group.topic.retries.exhausted").count() == 1
        metrics.counter("hermes-client.com_group.topic.retries.success").count() == 0
        metrics.summary("hermes-client.com_group.topic.retries.attempts").takeSnapshot().count() == 0
        metrics.timer("hermes-client.com_group.topic.latency").count() == 4
    }

    def "should update retries metrics"() {
        def retries = 3
        ReactiveHermesClient client = hermesClient(failingHermesSender(retries - 1))
                .withRetrySleep(0)
                .withRetries(retries)
                .withMetrics(metricsProvider).build()


        client.addMessageDeliveryListener(new MessageDeliveryListener() {
            @Override
            void onSend(HermesResponse response, long latency) {
                println("onSend: $latency")
            }

            @Override
            void onFailure(HermesMessage message, int attemptCount) {
                println("onFailure $attemptCount")
            }

            @Override
            void onFailedRetry(HermesMessage message, int attemptCount) {
                println("onFailedRetry $attemptCount")
            }

            @Override
            void onSuccessfulRetry(HermesMessage message, int attemptCount) {
                println("onSuccessfulRetry $attemptCount")
            }

            @Override
            void onMaxRetriesExceeded(HermesMessage message, int attemptCount) {
                println("onMaxRetriesExceeded $attemptCount")
            }
        })


        when:
        silence({ client.publish("com.group.topic", "123").block() })

        then:
        metrics.counter("hermes-client.com_group.topic.failure").count() == 2
        metrics.counter("hermes-client.com_group.topic.retries.exhausted").count() == 0
        metrics.counter("hermes-client.com_group.topic.retries.success").count() == 1
        metrics.counter("hermes-client.com_group.topic.status.{code}", "code", String.valueOf(201)).count() == 1
        metrics.counter("hermes-client.com_group.topic.retries.count").count() == 2
        metrics.summary("hermes-client.com_group.topic.retries.attempts").takeSnapshot().percentileValues()[0].value() == 2
        metrics.summary("hermes-client.com_group.topic.retries.attempts").takeSnapshot().max() == 2
        metrics.timer("hermes-client.com_group.topic.latency").count() == 3
    }

    private Mono<HermesResponse> successMono(HermesMessage message) {
        return Mono.just(HermesResponseBuilder.hermesResponse(message).withHttpStatus(201).build())
    }

    private Mono<HermesResponse> failingMono(Throwable throwable) {
        return Mono.error(throwable)
    }

    private ReactiveHermesSender failingHermesSender(int errorNo) {
        new ReactiveHermesSender() {
            int i = 0
            @Override
            Mono<HermesResponse> sendReactively(URI uri, HermesMessage message) {
                i++
                if (i <= errorNo) {
                    return failingMono(new RuntimeException())
                }
                return successMono(message)
            }
        }
    }

    private ReactiveHermesSender delayedHermesSender(Duration sendLatencyMs) {
        new ReactiveHermesSender() {
            @Override
            Mono<HermesResponse> sendReactively(URI uri, HermesMessage message) {
                Thread.sleep(sendLatencyMs.toMillis())
                return successMono(message)
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
