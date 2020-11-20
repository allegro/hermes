package pl.allegro.tech.hermes.client

import com.codahale.metrics.MetricRegistry
import pl.allegro.tech.hermes.client.metrics.DropwizardMetricsProvider
import pl.allegro.tech.hermes.client.metrics.MetricsProvider
import reactor.core.publisher.Mono
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit

import static pl.allegro.tech.hermes.client.ReactiveHermesClientBuilder.hermesClient

class ReactiveHermesClientMetricsTest extends Specification {

    private final MetricRegistry metrics = new MetricRegistry()
    private final MetricsProvider metricsProvider = new DropwizardMetricsProvider(metrics)

    def "should measure publish latency"() {
        given:
        ReactiveHermesClient client = hermesClient(delayedHermesSender(Duration.ofMillis(100)))
                .withRetrySleep(0)
                .withMetrics(metricsProvider).build()

        when:
        client.publish("com.group.topic", "123").block()

        then:
        metrics.counter("hermes-client.com_group.topic.status.201").count == 1
        metrics.timer("hermes-client.com_group.topic.latency").getSnapshot().getMax() >= Duration.ofMillis(100).get(ChronoUnit.NANOS)
        metrics.timer("hermes-client.com_group.topic.latency").getSnapshot().getMax() < Duration.ofMillis(300).get(ChronoUnit.NANOS)
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
        metrics.counter("hermes-client.com_group.topic.failure").count == 4
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")

        metrics.counter("hermes-client.com_group.topic.publish.failure").count == 4
        metrics.counter("hermes-client.com_group.topic.publish.finally.failure").count == 1
        metrics.counter("hermes-client.com_group.topic.publish.retry.failure").count == 3
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
        metrics.counter("hermes-client.com_group.topic.failure").count == 4
        metrics.counter("hermes-client.com_group.topic.retries.count").count == 3
        metrics.counter("hermes-client.com_group.topic.retries.exhausted").count == 1
        metrics.counter("hermes-client.com_group.topic.retries.success").count == 0
        metrics.histogram("hermes-client.com_group.topic.retries.attempts").getSnapshot().size() == 0
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")

        metrics.counter("hermes-client.com_group.topic.publish.finally.success").count == 0
        metrics.counter("hermes-client.com_group.topic.publish.finally.failure").count == 1
        metrics.counter("hermes-client.com_group.topic.publish.failure").count == 4
        metrics.counter("hermes-client.com_group.topic.publish.attempt").count == 1
        metrics.counter("hermes-client.com_group.topic.publish.retry.success").count == 0
        metrics.counter("hermes-client.com_group.topic.publish.retry.failure").count == 3
        metrics.counter("hermes-client.com_group.topic.publish.retry.attempt").count == 1
    }

    def "should update retries metrics"() {
        given:
        ReactiveHermesClient client1 = hermesClient(failingHermesSender(2))
                .withRetrySleep(0)
                .withRetries(4)
                .withMetrics(metricsProvider).build()

        ReactiveHermesClient client2 = hermesClient(failingHermesSender(5))
                .withRetrySleep(0)
                .withRetries(6)
                .withMetrics(metricsProvider).build()

        ReactiveHermesClient client3 = hermesClient(failingHermesSender(3))
                .withRetrySleep(0)
                .withRetries(2)
                .withMetrics(metricsProvider).build()

        when:
        silence({ client1.publish("com.group.topic", "123").block() })
        silence({ client2.publish("com.group.topic", "456").block() })
        silence({ client3.publish("com.group.topic", "789").block() })

        then:
        metrics.counter("hermes-client.com_group.topic.failure").count == 10
        metrics.counter("hermes-client.com_group.topic.retries.exhausted").count == 1
        metrics.counter("hermes-client.com_group.topic.retries.success").count == 2
        metrics.counter("hermes-client.com_group.topic.retries.count").count == 9
        metrics.histogram("hermes-client.com_group.topic.retries.attempts").getSnapshot().getMin() == 2
        metrics.histogram("hermes-client.com_group.topic.retries.attempts").getSnapshot().getMax() == 5
        metrics.timers.containsKey("hermes-client.com_group.topic.latency")

        metrics.counter("hermes-client.com_group.topic.publish.finally.success").count == 2
        metrics.counter("hermes-client.com_group.topic.publish.finally.failure").count == 1
        metrics.counter("hermes-client.com_group.topic.publish.failure").count == 10
        metrics.counter("hermes-client.com_group.topic.publish.attempt").count == 3
        metrics.counter("hermes-client.com_group.topic.publish.retry.success").count == 2
        metrics.counter("hermes-client.com_group.topic.publish.retry.failure").count == 9
        metrics.counter("hermes-client.com_group.topic.publish.retry.attempt").count == 3
    }

    def "should update failure metrics when there is an application-level error"() {
        given:
        ReactiveHermesClient client1 = hermesClient(badRequestHermesSender())
                .withRetrySleep(0)
                .withRetries(4)
                .withMetrics(metricsProvider).build()

        ReactiveHermesClient client2 = hermesClient(badRequestHermesSender())
                .withRetrySleep(0)
                .withRetries(6)
                .withMetrics(metricsProvider).build()

        ReactiveHermesClient client3 = hermesClient(badRequestHermesSender())
                .withRetrySleep(0)
                .withRetries(2)
                .withMetrics(metricsProvider).build()

        when:
        silence({ client1.publish("com.group.topic", "123").block() })
        silence({ client2.publish("com.group.topic", "456").block() })
        silence({ client3.publish("com.group.topic", "789").block() })

        then:
        metrics.counter("hermes-client.com_group.topic.publish.finally.success").count == 0
        metrics.counter("hermes-client.com_group.topic.publish.finally.failure").count == 3
        metrics.counter("hermes-client.com_group.topic.publish.failure").count == 3
        metrics.counter("hermes-client.com_group.topic.publish.attempt").count == 3
        metrics.counter("hermes-client.com_group.topic.publish.retry.success").count == 0
        metrics.counter("hermes-client.com_group.topic.publish.retry.failure").count == 0
        metrics.counter("hermes-client.com_group.topic.publish.retry.attempt").count == 0
    }

    private Mono<HermesResponse> successMono(HermesMessage message) {
        return Mono.just(HermesResponseBuilder.hermesResponse(message).withHttpStatus(201).build())
    }

    private Mono<HermesResponse> badRequestMono(HermesMessage message) {
        return Mono.just(HermesResponseBuilder.hermesResponse(message).withHttpStatus(400).build())
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

    private ReactiveHermesSender badRequestHermesSender() {
        new ReactiveHermesSender() {
            @Override
            Mono<HermesResponse> sendReactively(URI uri, HermesMessage message) {
                return badRequestMono(message)
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
