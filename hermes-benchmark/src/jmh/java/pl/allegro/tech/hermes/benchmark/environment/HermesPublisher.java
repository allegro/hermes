package pl.allegro.tech.hermes.benchmark.environment;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.IOReactorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HermesPublisher {
  private static final int CONNECT_TIMEOUT = 3000;
  private static final int SOCKET_TIMEOUT = 3000;
  private static final Logger logger = LoggerFactory.getLogger(HermesPublisher.class);

  private final CloseableHttpAsyncClient httpClient;
  private final URI targetUrl;
  private final HttpEntity body;

  public HermesPublisher(int maxConnectionsPerRoute, String targetUrl, String body)
      throws IOReactorException, UnsupportedEncodingException {
    this.targetUrl = URI.create(targetUrl);

    RequestConfig requestConfig =
        RequestConfig.custom()
            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
            .setAuthenticationEnabled(false)
            .build();

    IOReactorConfig ioReactorConfig =
        IOReactorConfig.custom()
            .setIoThreadCount(Runtime.getRuntime().availableProcessors())
            .setConnectTimeout(CONNECT_TIMEOUT)
            .setSoTimeout(SOCKET_TIMEOUT)
            .build();

    PoolingNHttpClientConnectionManager connectionManager =
        new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor(ioReactorConfig));
    connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);

    httpClient =
        HttpAsyncClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();

    httpClient.start();

    this.body = new StringEntity(body);
  }

  public int publish() {
    int response = 0;
    HttpPost httpPost = new HttpPost(targetUrl);
    httpPost.setEntity(body);
    httpPost.setHeader("Content-Type", "application/json");

    try {
      Future<HttpResponse> future = httpClient.execute(httpPost, null);
      response = future.get().getStatusLine().getStatusCode();
    } catch (RuntimeException | InterruptedException | ExecutionException exception) {
      logger.error("Client exception", exception);
    }
    return response;
  }

  public void stop() throws IOException {
    if (httpClient.isRunning()) {
      httpClient.close();
    }
  }
}
