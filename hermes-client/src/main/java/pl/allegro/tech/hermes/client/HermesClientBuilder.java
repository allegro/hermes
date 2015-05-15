package pl.allegro.tech.hermes.client;

import com.codahale.metrics.MetricRegistry;
import pl.allegro.tech.hermes.client.metrics.MetricsHermesSender;

import java.net.URI;
import java.util.function.Predicate;

import static java.net.HttpURLConnection.HTTP_CLIENT_TIMEOUT;

public class HermesClientBuilder {
    private HermesSender sender;
    private URI uri = URI.create("http://localhost:8080");
    private int retries = 0;
    private Predicate<HermesResponse> retryCondition = (res) ->
            res.getHttpStatus() == HTTP_CLIENT_TIMEOUT || res.getHttpStatus() / 100 == 5;

    public HermesClientBuilder(HermesSender sender) {
        this.sender = sender;
    }

    public static HermesClientBuilder hermesClient(HermesSender sender) {
        return new HermesClientBuilder(sender);
    }

    public HermesClient build() {
        return new HermesClient(sender, uri, retries, retryCondition);
    }

    public HermesClientBuilder withURI(URI uri) {
        this.uri = uri;
        return this;
    }

    public HermesClientBuilder withMetrics(MetricRegistry metrics) {
        this.sender = new MetricsHermesSender(sender, metrics);
        return this;
    }

    public HermesClientBuilder withRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public HermesClientBuilder withRetries(int retries, Predicate<HermesResponse> retryCondition) {
        this.retryCondition = retryCondition;
        return withRetries(retries);
    }
}
