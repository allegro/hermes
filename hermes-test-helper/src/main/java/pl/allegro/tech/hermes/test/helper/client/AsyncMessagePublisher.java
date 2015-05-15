package pl.allegro.tech.hermes.test.helper.client;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkArgument;
import static javax.ws.rs.client.Entity.text;

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
