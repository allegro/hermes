package pl.allegro.tech.hermes.consumers.consumer.sender.http

import com.codahale.metrics.MetricRegistry
import com.github.tomakehurst.wiremock.WireMockServer
import org.eclipse.jetty.client.HttpClient
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory
import pl.allegro.tech.hermes.consumers.config.ConsumerConfiguration
import pl.allegro.tech.hermes.consumers.config.Http2ClientProperties
import pl.allegro.tech.hermes.consumers.config.HttpClientProperties
import pl.allegro.tech.hermes.consumers.config.SslContextProperties
import pl.allegro.tech.hermes.metrics.PathsCompiler
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.post

class HttpClientConnectionMonitoringTest extends Specification {

    @Shared int port
    @Shared WireMockServer wireMock

    HttpClient client
    MetricRegistry metricRegistry = new MetricRegistry()
    HermesMetrics hermesMetrics = new HermesMetrics(metricRegistry, new PathsCompiler("localhost"))

    def setupSpec() {
        port = Ports.nextAvailable()
        wireMock = new WireMockServer(port)
        wireMock.start()
        wireMock.stubFor(post("/hello").willReturn(aResponse().withStatus(200)))
    }

    def setup() {
        SslContextFactoryProvider sslContextFactoryProvider = new SslContextFactoryProvider(null, new SslContextProperties())
        ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration()
        client = consumerConfiguration.http1Client(new HttpClientsFactory(
                new HttpClientProperties(),
                new Http2ClientProperties(),
                new InstrumentedExecutorServiceFactory(hermesMetrics),
                sslContextFactoryProvider))
        client.start()
    }

    def "should measure http client connections"() {
        given:
        def reporter = new HttpClientsWorkloadReporter(hermesMetrics, client, new Http2ClientHolder(null), false, true)
        reporter.start()

        when:
        client.POST("http://localhost:${port}/hello").send()

        and:
        def idle = metricRegistry.gauges['http-clients.http1.idle-connections'].value
        def active = metricRegistry.gauges['http-clients.http1.active-connections'].value

        then:
        idle + active > 0
    }

    def "should not register connection gauges for disabled http connection monitoring"() {
        given:
        def reporter = new HttpClientsWorkloadReporter(hermesMetrics, client, new Http2ClientHolder(null), false, false)

        when:
        reporter.start()

        then:
        metricRegistry.gauges.size() == 0
    }
}
