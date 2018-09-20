package pl.allegro.tech.hermes.management.infrastructure.graphite

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.junit.Rule
import org.springframework.web.client.RestTemplate
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Specification

import javax.ws.rs.core.MediaType

class RestTemplateGraphiteClientTest extends Specification {

    private static final int GRAPHITE_HTTP_PORT = Ports.nextAvailable()

    @Rule
    WireMockRule wireMockRule = new WireMockRule(GRAPHITE_HTTP_PORT)

    private RestTemplateGraphiteClient client

    void setup() {
        RestTemplate restTemplate = new RestTemplate();
        client = new RestTemplateGraphiteClient(restTemplate, URI.create("http://localhost:$GRAPHITE_HTTP_PORT"));
    }

    def "should get metrics for path"() {
        given:
        mockGraphite([
                [ metric: 'metric1', data: ['10'] ],
                [ metric: 'metric2', data: ['20'] ]
        ])

        when:
        GraphiteMetrics metrics = client.readMetrics("metric1", "metric2")

        then:
        metrics.metricValue("metric1") == "10"
        metrics.metricValue("metric2") == "20"
    }

    def "should return default value when metric has no value"() {
        given:
        mockGraphite([[ metric: 'metric', data: [null] ]])

        when:
        GraphiteMetrics metrics = client.readMetrics("metric");
        
        then:
        metrics.metricValue("metric1") == "0.0"
    }

    def "should return first notnull value"() {
        given:
        mockGraphite([
                [ metric: 'metric', data: [null, null, '13'] ],
        ])

        when:
        GraphiteMetrics metrics = client.readMetrics("metric");

        then:
        metrics.metricValue("metric") == "13"
    }

    def "should properly encode metric query strings"() {
        given:
        mockGraphite([
                [ metric: 'sumSeries%28stats.tech.hermes.%2A.m1_rate%29', data: ['13'] ],
        ])

        when:
        GraphiteMetrics metrics = client.readMetrics('sumSeries(stats.tech.hermes.*.m1_rate)');

        then:
        metrics.metricValue('sumSeries%28stats.tech.hermes.%2A.m1_rate%29') == "13"
    }

    private void mockGraphite(List queries) {
        String targetParams = queries.collect({ "target=$it.metric" }).join('&')
        String response = '[' + queries.collect({ jsonResponse(it.metric, it.data) }).join(',') + ']'

        mockGraphite(targetParams, response)
    }

    private void mockGraphite(String targetParams, String jsonResponse) {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(String.format("/render?from=-5minutes&until=-1minutes&format=json&%s", targetParams)))
                .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody(jsonResponse)));
    }

    private String jsonResponse(String query, List datapoints) {
        long timestamp = System.currentTimeSeconds()
        String datapointsString = datapoints.collect({ "[$it, $timestamp]" }).join(',')
        return '{"target": "' + query + '", "datapoints": [' + datapointsString + '], "tags": []}'
    }
    
}
