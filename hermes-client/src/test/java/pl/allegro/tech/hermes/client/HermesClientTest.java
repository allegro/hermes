package pl.allegro.tech.hermes.client;

import com.codahale.metrics.MetricRegistry;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.net.URI.create;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient;

public class HermesClientTest {

    private static final String HERMES_URI = "http://localhost:9999";

    private static final String TOPIC = "my.group.topicName";

    private static final String CONTENT = "{}";

    @Test
    public void shouldPublishMessageUsingSuppliedSender() throws MalformedURLException {
        // given
        int status = 201;
        HermesClient client = hermesClient((URI uri, HermesMessage message) -> {
            assertThat(uri.toString()).isEqualTo(HERMES_URI + "/topics/" + TOPIC);
            assertThat(message.getBody()).isEqualTo(CONTENT);
            return completedFuture(() -> status);
        }).withURI(create(HERMES_URI)).build();

        // when
        HermesResponse response = client.publish(TOPIC, CONTENT).join();

        // then
        assertThat(response.wasPublished()).isTrue();
        assertThat(response.wasAccepted()).isTrue();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isFailure()).isFalse();
        assertThat(response.getHttpStatus()).isEqualTo(status);
    }

    @Test
    public void shouldInterpretMessageAsAcceptedFor202() throws MalformedURLException {
        // given
        HermesClient client = hermesClient((uri, msg) -> completedFuture(() -> 202))
                .withURI(create(HERMES_URI)).build();

        // when
        HermesResponse response = client.publish(TOPIC, CONTENT).join();

        // then
        assertThat(response.wasPublished()).isFalse();
        assertThat(response.wasAccepted()).isTrue();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getHttpStatus()).isEqualTo(202);
    }

    @Test
    public void shouldInterpretMessageAsUnacceptedForAnythingOtherThan_202_or_201() throws MalformedURLException {
        asList(203, 204, 400, 401, 404, 500).forEach(
            // given
            status -> {
                HermesClient client = hermesClient((uri, msg) -> completedFuture(() -> status)).withURI(create(HERMES_URI)).build();

                // when
                HermesResponse response = client.publish(TOPIC, CONTENT).join();

                // then
                assertThat(response.wasPublished()).isFalse();
                assertThat(response.wasAccepted()).isFalse();
                assertThat(response.isSuccess()).isFalse();
                assertThat(response.isFailure()).isTrue();
            });
    }

    @Test
    public void shouldRegisterLatencyTimerViaMetricsInSanitizedPath() {
        // given
        int status = 201;
        MetricRegistry metrics = new MetricRegistry();
        HermesClient client = hermesClient((uri, msg) -> completedFuture(() -> status)).withMetrics(metrics).build();

        // when
        client.publish(TOPIC, CONTENT).join();

        // then
        assertThat(metrics.getCounters().get("hermes-client.my_group.topicName.status." + status).getCount()).isEqualTo(1);
        assertThat(metrics.getTimers()).containsKey("hermes-client.my_group.topicName.latency");
    }

    @Test
    public void shouldCloseTimerAfterCompleteExceptionally() {
        // given
        MetricRegistry metrics = new MetricRegistry();
        HermesClient client = hermesClient((uri, msg) -> {
            CompletableFuture<HermesResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException());
            return future;
        }).withMetrics(metrics).build();

        // when
        silence(() -> client.publish(TOPIC, CONTENT).join());

        // then
        assertThat(metrics.getTimers()).containsKey("hermes-client.my_group.topicName.latency");
    }

    @Test
     public void shouldRetryOnHttpFailure() {
        asList(408, 500, 501, 502, 503, 504, 505).forEach(
            status -> {
                // given
                final CountDownLatch latch = new CountDownLatch(5);
                HermesClient client = hermesClient(getCountDownSender(latch, status)).withRetries(5).build();

                // when
                client.publish(TOPIC, CONTENT).join();

                // then
                assertThat(latch.getCount()).isEqualTo(0);
            });
    }

    @Test
    public void shouldRetryOnSenderException() {
        IntStream.range(0, 10).forEach(
            attempt -> {
                // given
                final CountDownLatch latch = new CountDownLatch(5);
                HermesClient client = hermesClient(getExceptionallyFailingCountDownSender(latch)).withRetries(5).build();

                // when
                client.publish(TOPIC, CONTENT).join();

                // then
                assertThat(latch.getCount()).isEqualTo(0);
            });
    }

    @Test
    public void shouldUseSuppliedRetryCondition() {
        // given
        final CountDownLatch latch = new CountDownLatch(2);
        HermesClient client = hermesClient(getCountDownSender(latch, 777)).withRetries(5, (response) -> false).build();

        // when
        client.publish(TOPIC, CONTENT).join();

        // then
        assertThat(latch.getCount()).isEqualTo(1);
    }

    @Test
    public void shouldNotRetryWhenFinallyAccepted() {
        // given
        final CountDownLatch latch = new CountDownLatch(5);
        HermesClient client = hermesClient(getCountDownSender(latch, () -> latch.getCount() > 2 ? 408 : 201))
                .withRetries(5).build();

        // when
        client.publish(TOPIC, CONTENT).join();

        // then
        assertThat(latch.getCount()).isEqualTo(2);
    }

    private HermesSender getExceptionallyFailingCountDownSender(CountDownLatch latch) {
        return (uri, msg) -> {
            latch.countDown();
            CompletableFuture<HermesResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Sending failed"));
            return future;
        };
    }

    private HermesSender getCountDownSender(CountDownLatch latch, int status) {
        return getCountDownSender(latch, () -> status);
    }

    private HermesSender getCountDownSender(CountDownLatch latch, Supplier<Integer> status) {
        return (uri, msg) -> {
            latch.countDown();
            return completedFuture(status::get);
        };
    }

    private void silence(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception ex) {
            // do nothing
        }
    }
}
