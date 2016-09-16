package pl.allegro.tech.hermes.client

import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

import static java.net.URI.create
import static java.util.concurrent.CompletableFuture.completedFuture
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient

class HermesClientTest extends Specification {

    private static final String HERMES_URI = "http://localhost:9999"

    private static final String TOPIC = "my.group.topicName"

    private static final byte[] CONTENT = "{}".bytes

    private static final String CONTENT_TYPE = "application/json"

    private def executor = Executors.newFixedThreadPool(2)

    def "should publish message using supplied sender"() {
        given:
        HermesClient client = hermesClient({ URI uri, HermesMessage message ->
            assert uri.toString() == (String) "$HERMES_URI/topics/$TOPIC"
            assert message.body == CONTENT
            statusFuture(201)
        })
                .withURI(create(HERMES_URI))
                .build()

        when:
        HermesResponse response = client.publish(TOPIC, CONTENT_TYPE, CONTENT).join()

        then:
        response.success
        !response.failure
        response.httpStatus == 201
    }

    def "should interpret message as accepted when sender returns 202"() {
        given:
        HermesClient client = hermesClient({ uri, msg -> statusFuture(202) })
                .withURI(create(HERMES_URI))
                .build()

        when:
        HermesResponse response = client.publish(TOPIC, CONTENT_TYPE, CONTENT).join()

        then:
        response.success
        response.httpStatus == 202
    }

    def "should interpret message as failed for status different than 201 or 202"() {
        given:
        HermesClient client = hermesClient({uri, msg -> statusFuture(status)})
                .withURI(create(HERMES_URI))
                .withRetrySleep(0)
                .build()

         when:
         HermesResponse response = client.publish(TOPIC, CONTENT_TYPE, CONTENT).join()

         then:
         !response.success
         response.failure

        where:
        status << [203, 204, 400, 401, 404, 500]
    }


    def "should retry on http failure"() {
        given:
        CountDownLatch latch = new CountDownLatch(5)
        HermesClient client = hermesClient(getCountDownSender(latch, (Integer) status))
                .withRetries(5)
                .withRetrySleep(0)
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).join()
        then:
        latch.count == 0

        where:
        status << [408, 500, 501, 502, 503, 504, 505]
    }

    def "should retry on sender exception"() {
        given:
        CountDownLatch latch = new CountDownLatch(5)
        HermesClient client = hermesClient(getExceptionallyFailingCountDownSender(latch))
                .withRetries(5)
                .withRetrySleep(0)
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).join()

        then:
        latch.count == 0
    }

    def "should not retry when supplied retry condition says it should not retry"() {
        given:
        CountDownLatch latch = new CountDownLatch(2)
        HermesClient client = hermesClient(getCountDownSender(latch, 503))
                .withRetries(5, {false})
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).join()

        then:
        latch.count == 1
    }

    def "should not retry when one of the attempts succeeds to send"() {
        given:
        CountDownLatch latch = new CountDownLatch(5)
        HermesClient client = hermesClient(getCountDownSender(latch, {latch.getCount() > 2 ? 408 : 201}))
                .withRetries(5)
                .withRetrySleep(0)
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).join()

        then:
        latch.count == 2
    }

    def "should wait until all sent after shutdown"() {
        given:
        CountDownLatch latch = new CountDownLatch(5)
        HermesClient client = hermesClient(getCountDownDelayedSender(latch, 408, 20))
                .withRetries(5)
                .withRetrySleep(0)
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT)

        and:
        client.close(20, 1000)

        then:
        latch.await(1, TimeUnit.SECONDS)
    }

    def "should not publish after shutdown"() {
        given:
        HermesClient client = hermesClient({ uri, msg -> statusFuture(201) }).build()

        when:
        client.closeAsync(10).get(1, TimeUnit.SECONDS)
        def future = client.publish(TOPIC, CONTENT_TYPE, CONTENT)

        then:
        future.completedExceptionally
    }

    def "should keep retrying on sender exception after shutdown"() {
        given:
        CountDownLatch latch = new CountDownLatch(5)
        HermesClient client = hermesClient(getExceptionallyFailingCountDownSender(latch, 20))
                .withRetries(5)
                .withRetrySleep(0)
                .build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).join()

        and:
        client.closeAsync(50).get(1, TimeUnit.SECONDS)

        then:
        latch.await(1, TimeUnit.SECONDS)
    }

    def "should append default headers to message"() {
        given:
        Map<String, String> headers = [:]
        HermesClient client = hermesClient(getHeaderScrapingSender(headers))
            .withDefaultContentType('my/content')
            .withDefaultHeaderValue('Header', 'Value')
            .build()

        when:
        client.publish(HermesMessage.hermesMessage(TOPIC, CONTENT).build()).join()

        then:
        headers['Content-Type'] == 'my/content'
        headers['Header'] == 'Value'
    }

    def "should overwrite default headers when specific values provided"() {
        given:
        Map<String, String> headers = [:]
        HermesClient client = hermesClient(getHeaderScrapingSender(headers))
                .withDefaultContentType('my/content')
                .withDefaultHeaderValue('Header', 'Value')
                .build()

        when:
        client.publish(HermesMessage.hermesMessage(TOPIC, CONTENT)
                .json()
                .withHeader('Header', 'OtherValue')
                .build()).join()

        then:
        headers['Content-Type'] == 'application/json;charset=UTF-8'
        headers['Header'] == 'OtherValue'
    }

    def "should retry on sender exception when retry sleep is provided"() {
        given:
            CountDownLatch latch = new CountDownLatch(2)
            HermesClient client = hermesClient(getExceptionallyFailingCountDownSender(latch))
                    .withRetries(2)
                    .withRetrySleep(10)
                    .build()

        when:
            client.publish(HermesMessage.hermesMessage(TOPIC, CONTENT).build()).join()

        then:
            latch.count == 0
    }

    private HermesSender getExceptionallyFailingCountDownSender(CountDownLatch latch, long delay) {
        { uri, msg ->
            def future = new CompletableFuture()

            executor.submit({
                Thread.sleep(delay)
                latch.countDown()
                future.completeExceptionally(new RuntimeException("Sending failed"))
            } as Runnable)

            future
        }
    }

    private HermesSender getExceptionallyFailingCountDownSender(CountDownLatch latch) {
        { uri, msg ->
            latch.countDown()
            failingFuture(new RuntimeException("Sending failed"))
        }
    }

    private CompletableFuture<HermesResponse> statusFuture(int status) {
        completedFuture({status} as HermesResponse)
    }

    private CompletableFuture<HermesResponse> failingFuture(Throwable throwable) {
        CompletableFuture<HermesResponse> future = new CompletableFuture<>()
        future.completeExceptionally(throwable)
        future
    }

    private HermesSender getCountDownSender(CountDownLatch latch, int status) {
        getCountDownSender(latch, { status } as Supplier<Integer>)
    }

    private HermesSender getCountDownSender(CountDownLatch latch, Supplier<Integer> status) {
        { uri, msg ->
            latch.countDown()
            completedFuture({ status.get() } as HermesResponse)
        }
    }

    private HermesSender getCountDownDelayedSender(CountDownLatch latch, int status, long delay) {
        return { uri, msg ->
            def future = new CompletableFuture()

            executor.submit({
                Thread.sleep(delay)
                latch.countDown()
                future.complete({status} as HermesResponse)
            } as Runnable)

            future
        }
    }

    private HermesSender getHeaderScrapingSender(Map<String, String> headers) {
        { uri, msg ->
            headers.putAll(msg.headers)
            completedFuture({ 201 } as HermesResponse)
        }
    }
}
