package pl.allegro.tech.hermes.integration.helper;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Iterables;
import pl.allegro.tech.hermes.integration.env.EnvironmentAware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class AuditEventEndpoint implements EnvironmentAware {

    private static final String AUDIT_EVENT_URL = "/audit-events";

    private final List<LoggedRequest> receivedRequests = Collections.synchronizedList(new ArrayList<>());
    private final WireMock auditEventListener;
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

        auditEventListener.register(post(urlEqualTo(AUDIT_EVENT_URL))
                .willReturn(aResponse().withStatus(201)));
    }

    public LoggedRequest getLastRequest() {
        synchronized (receivedRequests) {
            return Iterables.getLast(receivedRequests);
        }
    }

    public void reset() {
        receivedRequests.clear();
    }

    public void stop() {
        auditEventListener.shutdown();
        service.shutdown();
    }

}
