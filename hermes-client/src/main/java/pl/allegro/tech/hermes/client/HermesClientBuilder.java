package pl.allegro.tech.hermes.client;

import com.codahale.metrics.MetricRegistry;
import pl.allegro.tech.hermes.client.metrics.MetricsHermesSender;

import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import java.util.function.Predicate;

public class HermesClientBuilder {

    private HermesSender sender;
    private URI uri = URI.create("http://localhost:8080");
    private final Map<String, String> defaultHeaders = new HashMap<>();
    private int retries = 3;
    private Predicate<HermesResponse> retryCondition = new HermesClientBasicRetryCondition();

    public HermesClientBuilder(HermesSender sender) {
        this.sender = sender;
        this.defaultHeaders.put(HermesMessage.CONTENT_TYPE_HEADER, HermesMessage.APPLICATION_JSON);
    }

    public static HermesClientBuilder hermesClient(HermesSender sender) {
        return new HermesClientBuilder(sender);
    }

    public HermesClient build() {
        return new HermesClient(sender, uri, defaultHeaders, retries, retryCondition);
    }

    public HermesClientBuilder withURI(URI uri) {
        this.uri = uri;
        return this;
    }

    public HermesClientBuilder withMetrics(MetricRegistry metrics) {
        this.sender = new MetricsHermesSender(sender, metrics);
        return this;
    }

    public HermesClientBuilder withDefaultContentType(String defaultContentType) {
        defaultHeaders.put(HermesMessage.CONTENT_TYPE_HEADER, defaultContentType);
        return this;
    }

    public HermesClientBuilder withDefaultHeaderValue(String header, String value) {
        defaultHeaders.put(header, value);
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
