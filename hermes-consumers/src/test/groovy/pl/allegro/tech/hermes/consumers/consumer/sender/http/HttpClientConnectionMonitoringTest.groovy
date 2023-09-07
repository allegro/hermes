package pl.allegro.tech.hermes.consumers.consumer.sender.http

import com.codahale.metrics.MetricRegistry
import com.github.tomakehurst.wiremock.WireMockServer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.search.Search
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.eclipse.jetty.client.HttpClient
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory
import pl.allegro.tech.hermes.common.metric.executor.ThreadPoolMetrics
import pl.allegro.tech.hermes.consumers.config.ConsumerSenderConfiguration
import pl.allegro.tech.hermes.consumers.config.Http1ClientProperties
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
    HttpClient batchClient
    MetricRegistry metricRegistry = new MetricRegistry()
    HermesMetrics hermesMetrics = new HermesMetrics(metricRegistry, new PathsCompiler("localhost"))
    MeterRegistry meterRegistry = new SimpleMeterRegistry()
    MetricsFacade metrics = new MetricsFacade(meterRegistry, hermesMetrics)
    ThreadPoolMetrics threadPoolMetrics = new ThreadPoolMetrics(metrics)

    def setupSpec() {
        port = Ports.nextAvailable()
        wireMock = new WireMockServer(port)
        wireMock.start()
        wireMock.stubFor(post("/hello").willReturn(aResponse().withStatus(200)))
    }

    def setup() {
        SslContextFactoryProvider sslContextFactoryProvider = new SslContextFactoryProvider(null, new SslContextProperties())
        ConsumerSenderConfiguration consumerConfiguration = new ConsumerSenderConfiguration();
        client = consumerConfiguration.http1SerialClient(new HttpClientsFactory(
                new InstrumentedExecutorServiceFactory(threadPoolMetrics),
                sslContextFactoryProvider), new Http1ClientProperties()
        )
        batchClient = Mock(HttpClient)
        client.start()
    }

    def "should measure http client connections"() {
        given:
        def reporter = new HttpClientsWorkloadReporter(metrics, client, batchClient, new Http2ClientHolder(null), false, true)
        reporter.start()

        when:
        client.POST("http://localhost:${port}/hello").send()

        and:
        def idleDropwizard = metricRegistry.gauges['http-clients.serial.http1.idle-connections'].value
        def activeDropwizard = metricRegistry.gauges['http-clients.serial.http1.active-connections'].value
        def idleMicrometer = Search.in(meterRegistry).name("http-clients.serial.http1.idle-connections").gauge().value()
        def activeMicrometer = Search.in(meterRegistry).name("http-clients.serial.http1.active-connections").gauge().value()

        then:
        idleDropwizard + activeDropwizard > 0
        idleMicrometer + activeMicrometer > 0
    }

    def "should not register connection gauges for disabled http connection monitoring"() {
        given:
        def reporter = new HttpClientsWorkloadReporter(metrics, client, batchClient, new Http2ClientHolder(null), false, false)

        when:
        reporter.start()

        then:
        metricRegistry.gauges.size() == 0
        Search.in(meterRegistry).gauges().size() == 0
    }
}
