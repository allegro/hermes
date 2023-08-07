package pl.allegro.tech.hermes.test.helper.client;

import jakarta.ws.rs.client.AsyncInvoker;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkArgument;
import static jakarta.ws.rs.client.Entity.text;

public class AsyncMessagePublisher {

    private WebTarget target;

    public AsyncMessagePublisher(WebTarget target) {
        this.target = target;
    }

    public void publishMessage(String topic, String content, InvocationCallback<Response> callback) {
        target.path(topic).request().async().post(text(content), callback);
    }

    public void publishMessage(String topic, String content, int retries, InvocationCallback<Response> callback) {
        checkArgument(retries > 0, "Retries should be more than zero.");
        publishMessage(topic, text(content), retries, callback);
    }

    private void publishMessage(String topic, Entity<String> content, int retries, InvocationCallback<Response> callback) {
        AsyncInvoker invoker = target.path(topic).request().async();
        invoker.post(content, new PublishingRetryCallback(invoker, content, retries, callback));
    }
}
