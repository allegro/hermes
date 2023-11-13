package pl.allegro.tech.hermes.integrationtests.subscriber;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.integrationtests.HermesAssertions.assertThat;

public class TestSubscriberTest {

    @RegisterExtension
    static TestSubscribersExtension subscribers = new TestSubscribersExtension();

    @Test
    public void shouldSendAndAssertMessageOnOnlyOneSubscriber() throws IOException {
        // given
        TestSubscriber subscriber1 = subscribers.createSubscriber();
        TestSubscriber subscriber2 = subscribers.createSubscriber();
        String jsonMessage = "{\"hello\": \"world\"}";

        // when
        HttpResponse response = postJson(subscriber1.getEndpoint(), jsonMessage, new BasicHeader("hermes-message-id", "123"));

        // then
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(OK.getStatusCode());
        subscriber1.waitUntilReceived(jsonMessage);
        subscriber1.waitUntilRequestReceived(request -> {
            assertThat(request).hasHeaderValue("content-type", "application/json");
            assertThat(request.getHeader("hermes-message-id")).isEqualTo("123");
        });
        subscriber2.noMessagesReceived();
    }

    private HttpResponse postJson(String endpoint, String jsonMessage, BasicHeader basicHeader) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(endpoint);
            post.setEntity(new StringEntity(jsonMessage));
            post.setHeader("content-type", "application/json");
            post.setHeader(basicHeader);
            return httpClient.execute(post);
        }
    }
}
