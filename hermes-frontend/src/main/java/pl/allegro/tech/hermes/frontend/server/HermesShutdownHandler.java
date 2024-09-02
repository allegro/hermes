package pl.allegro.tech.hermes.frontend.server;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;

public class HermesShutdownHandler implements HttpHandler {

  private static final Logger logger = LoggerFactory.getLogger(HermesShutdownHandler.class);

  private static final int MILLIS = 1000;
  private static final int MAX_INFLIGHT_RETRIES = 20;
  private static final int TOLERANCE_BYTES = 5;

  private final HttpHandler next;
  private final MetricsFacade metrics;
  private final ExchangeCompletionListener completionListener =
      new GracefulExchangeCompletionListener();
  private final AtomicInteger inflightRequests = new AtomicInteger();
  private volatile boolean shutdown = false;

  public HermesShutdownHandler(HttpHandler next, MetricsFacade metrics) {
    this.next = next;
    this.metrics = metrics;
    metrics.producer().registerProducerInflightRequestGauge(inflightRequests, AtomicInteger::get);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    if (shutdown) {
      exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
      exchange.endExchange();
      return;
    }
    exchange.addExchangeCompleteListener(completionListener);
    inflightRequests.incrementAndGet();
    next.handleRequest(exchange);
  }

  public void handleShutdown() throws InterruptedException {
    shutdown = true;
    logger.info("Waiting for inflight requests to complete");
    awaitRequestsComplete();
    logger.info("Awaiting buffer flush");
    awaitBufferFlush();
    logger.info("Shutdown complete");
  }

  private void awaitRequestsComplete() throws InterruptedException {
    int retries = MAX_INFLIGHT_RETRIES;
    while (inflightRequests.get() > 0 && retries > 0) {
      logger.info(
          "Inflight requests: {}, timing out in {} ms", inflightRequests.get(), retries * MILLIS);
      retries--;
      Thread.sleep(MILLIS);
    }
  }

  private void awaitBufferFlush() throws InterruptedException {
    while (!isBufferEmpty()) {
      Thread.sleep(MILLIS);
    }
  }

  private boolean isBufferEmpty() {
    long bufferUsedBytes =
        (long)
            (metrics.producer().getBufferTotalBytes()
                - metrics.producer().getBufferAvailableBytes());
    logger.info("Buffer flush: {} bytes still in use", bufferUsedBytes);
    return bufferUsedBytes < TOLERANCE_BYTES;
  }

  private final class GracefulExchangeCompletionListener implements ExchangeCompletionListener {

    @Override
    public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
      inflightRequests.decrementAndGet();
      nextListener.proceed();
    }
  }
}
