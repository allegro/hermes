package pl.allegro.tech.hermes.consumers.consumer.sender.http

import com.github.tomakehurst.wiremock.WireMockServer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.search.Search
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.eclipse.jetty.client.HttpClient
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory
import pl.allegro.tech.hermes.consumers.config.ConsumerSenderConfiguration
import pl.allegro.tech.hermes.consumers.config.Http1ClientProperties
import pl.allegro.tech.hermes.consumers.config.HttpClientsMonitoringProperties
import pl.allegro.tech.hermes.consumers.config.SslContextProperties
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.post

class HttpClientMonitoringTest extends Specification {

    @Shared
    int port
    @Shared
    WireMockServer wireMock

    HttpClient batchClient
    MeterRegistry meterRegistry = new SimpleMeterRegistry()
    MetricsFacade metrics = new MetricsFacade(meterRegistry)
    HttpClientsMonitoringProperties monitoringProperties = new HttpClientsMonitoringProperties()
    SslContextFactoryProvider sslContextFactoryProvider = new SslContextFactoryProvider(null, new SslContextProperties())
    ConsumerSenderConfiguration consumerConfiguration = new ConsumerSenderConfiguration();

    def setupSpec() {
        port = Ports.nextAvailable()
        wireMock = new WireMockServer(port)
        wireMock.start()
        wireMock.stubFor(post("/hello").willReturn(aResponse().withStatus(200)))
    }

    def setup() {
        batchClient = Mock(HttpClient)
    }

    def "should measure http client"() {
        given:
        def client = consumerConfiguration.http1SerialClient(new HttpClientsFactory(
                new InstrumentedExecutorServiceFactory(metrics),
                sslContextFactoryProvider), metrics, new Http1ClientProperties(), monitoringProperties
        )
        client.start()
        monitoringProperties.requestProcessingMonitoringEnabled = true
        def reporter = new HttpClientsWorkloadReporter(metrics, client, batchClient, new Http2ClientHolder(null), false, true)
        reporter.start()

        when:
        client.POST("http://localhost:${port}/hello").send()

        and:
        def idleMicrometer = Search.in(meterRegistry).name("http-clients.serial.http1.idle-connections").gauge().value()
        def activeMicrometer = Search.in(meterRegistry).name("http-clients.serial.http1.active-connections").gauge().value()
        def queueWaitTimer = Search.in(meterRegistry).name("http-clients.serial.http1.request-queue-waiting-time").timer().count()
        def requestProcessingTimer = Search.in(meterRegistry).name("http-clients.serial.http1.request-processing-time").timer().count()

        then:
        idleMicrometer + activeMicrometer > 0
        queueWaitTimer == 1
        requestProcessingTimer == 1
    }

    def "should not register metrics for disabled http client monitoring"() {
        given:
        monitoringProperties.requestProcessingMonitoringEnabled = false
        def client = consumerConfiguration.http1SerialClient(new HttpClientsFactory(
                new InstrumentedExecutorServiceFactory(metrics),
                sslContextFactoryProvider), metrics, new Http1ClientProperties(), monitoringProperties
        )
        client.start()
        def reporter = new HttpClientsWorkloadReporter(metrics, client, batchClient, new Http2ClientHolder(null), false, false)

        when:
        reporter.start()
        client.POST("http://localhost:${port}/hello").send()

        then:
        Search.in(meterRegistry).gauges().size() == 0
        Search.in(meterRegistry).name("http-clients.serial.http1.request-queue-waiting-time").timer() == null
        Search.in(meterRegistry).name("http-clients.serial.http1.request-processing-time").timer() == null
    }
}
