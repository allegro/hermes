package pl.allegro.tech.hermes.integrationtests.subscriber;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

public class TestSubscribersExtension implements AfterEachCallback, AfterAllCallback {

    private final WireMockServer service;
    private final URI serviceUrl;
    private final AtomicInteger subscriberIndex = new AtomicInteger();
    private final Map<String, TestSubscriber> subscribersPerPath = new ConcurrentHashMap<>();

    public TestSubscribersExtension() {
        service = new WireMockServer(0);
        service.start();
        serviceUrl = URI.create("http://localhost:" + service.port());
        service.addStubMapping(
            post(UrlPattern.ANY).willReturn(aResponse().withStatus(200)).build());
        service.addMockServiceRequestListener((request, response) -> {
            TestSubscriber subscriber = subscribersPerPath.get(request.getUrl()); // getUrl() returns path here
            if (subscriber != null) {
                subscriber.onRequestReceived(LoggedRequest.createFrom(request));
            }
        });
    }

    public TestSubscriber createSubscriber() {
        String path = "/subscriber-" + subscriberIndex.incrementAndGet();
        TestSubscriber subscriber = new TestSubscriber(createSubscriberURI(path));
        subscribersPerPath.put(path, subscriber);
        return subscriber;
    }

    private URI createSubscriberURI(String path) {
        return serviceUrl.resolve(path);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        service.resetRequests();
        subscribersPerPath.clear();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        service.stop();
    }
}
