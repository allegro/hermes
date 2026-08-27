package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.Request;
import pl.allegro.tech.hermes.metrics.HermesTimer;

public class JettyHttpClientMetrics implements Request.Listener {

  private final HermesTimer requestQueueWaitingTimer;
  private final HermesTimer requestProcessingTimer;

  public JettyHttpClientMetrics(
      HermesTimer requestQueueWaitingTimer, HermesTimer requestProcessingTimer) {
    this.requestQueueWaitingTimer = requestQueueWaitingTimer;
    this.requestProcessingTimer = requestProcessingTimer;
  }

  @Override
  public void onQueued(Request request) {
    var timer = requestQueueWaitingTimer.time();

    request.onRequestBegin(onBeginRequest -> timer.close());
  }

  @Override
  public void onBegin(Request request) {
    var timer = requestProcessingTimer.time();

    request.onComplete(result -> timer.close());
  }
}
