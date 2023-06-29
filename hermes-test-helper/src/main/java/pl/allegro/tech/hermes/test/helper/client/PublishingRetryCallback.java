package pl.allegro.tech.hermes.test.helper.client;

import jakarta.ws.rs.client.AsyncInvoker;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkArgument;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;

class PublishingRetryCallback implements InvocationCallback<Response> {
    private final AsyncInvoker invoker;
    private final Entity<String> message;
    private final InvocationCallback<Response> callback;
    private int retries;

    PublishingRetryCallback(AsyncInvoker invoker,
                            Entity<String> message,
                            int retries,
                            InvocationCallback<Response> callback) {
        checkArgument(retries > 0);
        this.invoker = invoker;
        this.message = message;
        this.retries = retries;
        this.callback = callback;
    }

    @Override
    public void completed(Response response) {
        if (SUCCESSFUL.equals(response.getStatusInfo().getFamily()) || retries <= 0) {
            callback.completed(response);
        } else {
            retry();
        }
    }

    @Override
    public void failed(Throwable throwable) {
        if (retries > 0) {
            retry();
        } else {
            callback.failed(throwable);
        }
    }

    private void retry() {
        retries--;
        invoker.post(message, this);
    }
}
