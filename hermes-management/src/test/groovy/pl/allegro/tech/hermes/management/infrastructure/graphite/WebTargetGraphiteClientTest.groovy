package pl.allegro.tech.hermes.management.infrastructure.graphite

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.junit.Rule
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Specification

import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType

class WebTargetGraphiteClientTest extends Specification {

    private static final int GRAPHITE_HTTP_PORT = Ports.nextAvailable()

    @Rule
    WireMockRule wireMockRule = new WireMockRule(GRAPHITE_HTTP_PORT)

    private WebTargetGraphiteClient client

    void setup() {
        WebTarget webTarget = ClientBuilder
                .newClient()
                .register(JacksonJsonProvider.class)
                .target("http://localhost:$GRAPHITE_HTTP_PORT")
        client = new WebTargetGraphiteClient(webTarget);
    }

    def "should get metrics for path"() {
        given:
        mockGraphite(
                "target=metric1&target=metric2",
                arrayJsonResponse("metric1", "metric2", "10", "20")
        );

        when:
        GraphiteMetrics metrics = client.readMetrics("metric1", "metric2")

        then:
        metrics.metricValue("metric1") == "10"
        metrics.metricValue("metric2") == "20"
    }

    def "should return default value when metric has no value"() {
        given:
        mockGraphite(
                "target=metric",
                arrayJsonResponse("metric", null)
        );

        when:
        GraphiteMetrics metrics = client.readMetrics("metric");
        
        then:
        metrics.metricValue("metric1") == "0.0"
    }
    
    private void mockGraphite(String targetParams, String jsonResponse) {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(String.format("/render?from=-1minutes&until=now&format=json&%s", targetParams)))
                .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody(jsonResponse)));
    }

    private String arrayJsonResponse(String query1, String query2, String rate1, String rate2) {
        return String.format("[%s,%s]", jsonResponse(query1, rate1), jsonResponse(query2, rate2));
    }

    private String arrayJsonResponse(String query, String rate) {
        return String.format("[%s]", jsonResponse(query, rate));
    }

    private String jsonResponse(String query, String rate) {
        return String.format("{\"target\": \"%s\", \"datapoints\": [[%s, %s]]}", query, rate, new Date().getTime());
    }
    
}
