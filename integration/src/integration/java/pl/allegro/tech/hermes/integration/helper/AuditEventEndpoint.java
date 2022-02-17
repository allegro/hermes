package pl.allegro.tech.hermes.integration.helper;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Iterables;
import com.jayway.awaitility.Duration;
import pl.allegro.tech.hermes.integration.env.EnvironmentAware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class AuditEventEndpoint implements EnvironmentAware {

    private static final String AUDIT_EVENT_URL = "/audit-events";
    private final WireMock auditEventListener;

    private final List<LoggedRequest> receivedRequests = Collections.synchronizedList(new ArrayList<>());

    private final WireMockServer service;

    public AuditEventEndpoint(WireMockServer wireMockServer) {
        this.auditEventListener = new WireMock("localhost", wireMockServer.port());
        this.service = wireMockServer;

        service.resetMappings();
        service.resetRequests();
        service.resetScenarios();

        service.addMockServiceRequestListener((request, response) -> {
            if (AUDIT_EVENT_URL.equals(request.getUrl())) {
                receivedRequests.add(LoggedRequest.createFrom(request));
            }
        });

        auditEventListener
                .register(
                        post(urlEqualTo(AUDIT_EVENT_URL))
                                .willReturn(aResponse().withStatus(201)));
    }

    public void waitUntilReceived(long seconds) {
        await().atMost(adjust(new Duration(seconds, TimeUnit.SECONDS))).until(() ->
                assertThat(receivedRequests.size()).isGreaterThanOrEqualTo(1));
    }

    private LoggedRequest getLastReceivedRequest() {
        synchronized (receivedRequests) {
            return Iterables.getLast(receivedRequests);
        }
    }

    public LoggedRequest waitAndGetLastRequest() {
        waitUntilReceived(60);
        return getLastReceivedRequest();
    }

    public void reset() {
        receivedRequests.clear();
    }

    public void stop() {
        auditEventListener.shutdown();
        service.shutdown();
    }
}
