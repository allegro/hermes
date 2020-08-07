package pl.allegro.tech.hermes.client

import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

import static java.net.URI.create
import static pl.allegro.tech.hermes.client.ReactiveHermesClientBuilder.hermesClient

class ReactiveHermesClientTest extends Specification {

    private static final String HERMES_URI = "http://localhost:9999"

    private static final String TOPIC = "my.group.topicName"

    private static final byte[] CONTENT = "{}".bytes

    private static final String CONTENT_TYPE = "application/json"

    private def executor = Executors.newFixedThreadPool(2)

    def "should publish message using supplied sender"() {
        given:
        ReactiveHermesClient client = hermesClient({ URI uri, HermesMessage message ->
            assert uri.toString() == (String) "$HERMES_URI/topics/$TOPIC"
            assert message.body == CONTENT
            statusFuture(message, 201)
        })
                .withURI(create(HERMES_URI))
                .build()

        when:
        HermesResponse response = client.publish(TOPIC, CONTENT_TYPE, CONTENT).block()

        then:
        response.success
        !response.failure
        response.httpStatus == 201
        !response.failureCause.isPresent()
    }

    def "should interpret message as accepted when sender returns 202"() {
        given:
        ReactiveHermesClient client = hermesClient({ uri, msg -> statusFuture(msg, 202) })
                .withURI(create(HERMES_URI))
                .build()

        when:
        HermesResponse response = client.publish(TOPIC, CONTENT_TYPE, CONTENT).block()

        then:
        response.success
        response.httpStatus == 202
    }

    def "should interpret message as failed for status different than 201 or 202"() {
        given:
        ReactiveHermesClient client = hermesClient({uri, msg -> statusFuture(msg, status)})
                .withURI(create(HERMES_URI))
                .withRetrySleep(0)
                .build()

        when:
        HermesResponse response = client.publish(TOPIC, CONTENT_TYPE, CONTENT).block()

        then:
        !response.success
        response.failure

        where:
        status << [203, 204, 400, 401, 404, 500]
    }


    def "should retry on http failure"() {
        given:
        CountDownLatch latch = new CountDownLatch(5)
        ReactiveHermesClient client = hermesClient(getCountDownSender(latch, (Integer) status))
                .withRetries(5)
                .withRetrySleep(0)
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).block()
        then:
        latch.count == 0

        where:
        status << [408, 500, 501, 502, 503, 504, 505]
    }

    def "should return failed message in case of failed sending"() {
        given:
        CountDownLatch latch = new CountDownLatch(5)
        ReactiveHermesClient client = hermesClient(getExceptionallyFailingCountDownSender(latch))
                .withRetries(5)
                .withRetrySleep(0)
                .build()

        when:
        def response = client.publish(TOPIC, CONTENT_TYPE, CONTENT).block()

        then:
        latch.count == 0
        response.hermesMessage.body == CONTENT
        response.hermesMessage.contentType == CONTENT_TYPE
        response.hermesMessage.topic == TOPIC
        response.failureCause.get().message == "Sending failed"
    }

    def "should retry on sender exception"() {
        given:
        CountDownLatch latch = new CountDownLatch(5)
        ReactiveHermesClient client = hermesClient(getExceptionallyFailingCountDownSender(latch))
                .withRetries(5)
                .withRetrySleep(0)
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).block()

        then:
        latch.count == 0
    }

    def "should not retry when supplied retry condition says it should not retry"() {
        given:
        CountDownLatch latch = new CountDownLatch(2)
        ReactiveHermesClient client = hermesClient(getCountDownSender(latch, 503))
                .withRetries(5, {false})
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).block()

        then:
        latch.count == 1
    }

    def "should not retry when one of the attempts succeeds to send"() {
        given:
        CountDownLatch latch = new CountDownLatch(5)
        ReactiveHermesClient client = hermesClient(getCountDownSender(latch, {latch.getCount() > 2 ? 408 : 201}))
                .withRetries(5)
                .withRetrySleep(0)
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).block()

        then:
        latch.count == 2
    }

    def "should wait until all sent after shutdown"() {
        given:
        CountDownLatch latch = new CountDownLatch(5)
        ReactiveHermesClient client = hermesClient(getCountDownDelayedSender(latch, 408, 20))
                .withRetries(5)
                .withRetrySleep(0)
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT)
                .subscribeOn(Schedulers.elastic())
                .subscribe()

        and:
        client.close(20, 1000)

        then:
        latch.await(1, TimeUnit.SECONDS)
    }

    def "should not publish after shutdown"() {
        given:
        ReactiveHermesClient client = hermesClient({ uri, msg -> statusFuture(msg, 201) }).build()

        when:
        client.closeAsync(10).block(Duration.ofSeconds(1))
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).block()

        then:
        thrown(HermesClientShutdownException)
    }

    def "should keep retrying on sender exception after shutdown"() {
        given:
        CountDownLatch latch = new CountDownLatch(5)
        ReactiveHermesClient client = hermesClient(getExceptionallyFailingCountDownSender(latch, 20))
                .withRetries(5)
                .withRetrySleep(0)
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT)
            .subscribeOn(Schedulers.elastic())
            .subscribe()

        and:
        client.closeAsync(50).block(Duration.ofSeconds(1))

        then:
        latch.await(1, TimeUnit.SECONDS)
    }

    def "should append default headers to message"() {
        given:
        Map<String, String> headers = [:]
        ReactiveHermesClient client = hermesClient(getHeaderScrapingSender(headers))
            .withDefaultContentType('my/content')
            .withDefaultHeaderValue('Header', 'Value')
            .build()

        when:
        client.publish(HermesMessage.hermesMessage(TOPIC, CONTENT).build()).block()

        then:
        headers['Content-Type'] == 'my/content'
        headers['Header'] == 'Value'
    }

    def "should overwrite default headers when specific values provided"() {
        given:
        Map<String, String> headers = [:]
        ReactiveHermesClient client = hermesClient(getHeaderScrapingSender(headers))
                .withDefaultContentType('my/content')
                .withDefaultHeaderValue('Header', 'Value')
                .build()

        when:
        client.publish(HermesMessage.hermesMessage(TOPIC, CONTENT)
                .json()
                .withHeader('Header', 'OtherValue')
                .build()).block()

        then:
        headers['Content-Type'] == 'application/json;charset=UTF-8'
        headers['Header'] == 'OtherValue'
    }

    def "should retry on sender exception when retry sleep is provided"() {
        given:
            CountDownLatch latch = new CountDownLatch(2)
            ReactiveHermesClient client = hermesClient(getExceptionallyFailingCountDownSender(latch))
                    .withRetries(2)
                    .withRetrySleep(10)
                    .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).block()

        then:
            latch.count == 0
    }

    def "should retry when sender throws exception"() {
        given:
        CountDownLatch latch = new CountDownLatch(2)
        ReactiveHermesClient client = hermesClient(getThrowingCountDownSender(latch))
                .withRetries(2)
                .withRetrySleep(10)
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).block()

        then:
        thrown(RuntimeException)
        latch.count == 0
    }

    private ReactiveHermesSender getExceptionallyFailingCountDownSender(CountDownLatch latch, long delay) {
        { uri, msg ->
            def future = new CompletableFuture()

            executor.submit({
                Thread.sleep(delay)
                latch.countDown()
                future.completeExceptionally(new RuntimeException("Sending failed"))
            } as Runnable)

            Mono.fromFuture(future)
        }
    }

    private ReactiveHermesSender getExceptionallyFailingCountDownSender(CountDownLatch latch) {
        { uri, msg ->
            Mono.fromCallable {
                latch.countDown()
                throw new RuntimeException("Sending failed")
            }
        }
    }

    private ReactiveHermesSender getThrowingCountDownSender(CountDownLatch latch) {
        { uri, msg ->
            latch.countDown()
            throw new RuntimeException("Sending failed")
        }
    }

    private Mono<HermesResponse> statusFuture(HermesMessage message, int status) {
        Mono.just(HermesResponseBuilder.hermesResponse(message).withHttpStatus(status).build())
    }

    private ReactiveHermesSender getCountDownSender(CountDownLatch latch, int status) {
        getCountDownSender(latch, { status } as Supplier<Integer>)
    }

    private ReactiveHermesSender getCountDownSender(CountDownLatch latch, Supplier<Integer> status) {
        { uri, msg ->
            Mono.fromCallable  {
                latch.countDown()
                return HermesResponseBuilder.hermesResponse(msg).withHttpStatus(status.get()).build()
            }
        }
    }

    private ReactiveHermesSender getCountDownDelayedSender(CountDownLatch latch, int status, long delay) {
        return { uri, msg ->
            def future = new CompletableFuture()

            executor.submit({
                Thread.sleep(delay)
                latch.countDown()
                future.complete(HermesResponseBuilder.hermesResponse(msg).withHttpStatus(status).build())
            } as Runnable)

            Mono.fromFuture(future)
        }
    }

    private ReactiveHermesSender getHeaderScrapingSender(Map<String, String> headers) {
        { uri, msg ->
            headers.putAll(msg.headers)
            Mono.just(HermesResponseBuilder.hermesResponse(msg).withHttpStatus(201).build())
        }
    }
}
