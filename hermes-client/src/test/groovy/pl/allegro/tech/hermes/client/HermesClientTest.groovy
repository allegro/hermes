package pl.allegro.tech.hermes.client

import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.function.Supplier

import static java.net.URI.create
import static java.util.concurrent.CompletableFuture.completedFuture
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient

class HermesClientTest extends Specification {

    private static final String HERMES_URI = "http://localhost:9999"

    private static final String TOPIC = "my.group.topicName"

    private static final byte[] CONTENT = "{}".bytes

    private static final String CONTENT_TYPE = "application/json"

    def "should publish message using supplied sender"() {
        given:
        HermesClient client = hermesClient({ URI uri, HermesMessage message ->
            assert uri.toString() == (String) "$HERMES_URI/topics/$TOPIC"
            assert message.body == CONTENT
            return statusFuture(201)
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
        HermesClient client = hermesClient({uri, msg -> statusFuture(status)}).withURI(create(HERMES_URI)).build()

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
        HermesClient client = hermesClient(getCountDownSender(latch, (Integer) status)).withRetries(5).build()

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
        HermesClient client = hermesClient(getExceptionallyFailingCountDownSender(latch)).withRetries(5).build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).join()

        then:
        latch.count == 0
    }

    def "should not retry when supplied retry condition says it should not retry"() {
        given:
        CountDownLatch latch = new CountDownLatch(2)
        HermesClient client = hermesClient(getCountDownSender(latch, 503)).withRetries(5, {false}).build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).join()

        then:
        latch.count == 1
    }

    def "should not retry when one of the attempts succeeds to send"() {
        given:
        CountDownLatch latch = new CountDownLatch(5)
        HermesClient client = hermesClient(getCountDownSender(latch, {latch.getCount() > 2 ? 408 : 201})).withRetries(5).build()

        when:
        client.publish(TOPIC, CONTENT_TYPE, CONTENT).join()

        then:
        latch.count == 2
    }

    private HermesSender getExceptionallyFailingCountDownSender(CountDownLatch latch) {
        return { uri, msg ->
            latch.countDown();
            return failingFuture(new RuntimeException("Sending failed"));
        };
    }

    private CompletableFuture<HermesResponse> statusFuture(int status) {
        return completedFuture({status} as HermesResponse)
    }

    private CompletableFuture<HermesResponse> failingFuture(Throwable throwable) {
        CompletableFuture<HermesResponse> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    private HermesSender getCountDownSender(CountDownLatch latch, int status) {
        return getCountDownSender(latch, { status } as Supplier<Integer>);
    }

    private HermesSender getCountDownSender(CountDownLatch latch, Supplier<Integer> status) {
        return { uri, msg ->
            latch.countDown();
            return completedFuture({ status.get() } as HermesResponse);
        };
    }
}
