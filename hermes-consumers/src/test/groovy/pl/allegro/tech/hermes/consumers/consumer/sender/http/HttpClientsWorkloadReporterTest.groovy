package pl.allegro.tech.hermes.consumers.consumer.sender.http

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.HttpDestination
import org.eclipse.jetty.client.HttpExchange
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import spock.lang.Specification

import java.util.concurrent.LinkedBlockingQueue

class HttpClientsWorkloadReporterTest extends Specification {

    def "should return sum of http/1 and http/2 clients destinations"() {
        given:
        def http1Destination1 = Mock(HttpDestination)
        http1Destination1.getHttpExchanges() >> new LinkedBlockingQueue<HttpExchange>([Mock(HttpExchange)])
        def http1Destination2 = Mock(HttpDestination)
        http1Destination2.getHttpExchanges() >> new LinkedBlockingQueue<HttpExchange>([Mock(HttpExchange), Mock(HttpExchange)])

        def http2Destination = Mock(HttpDestination)
        http2Destination.getHttpExchanges() >> new LinkedBlockingQueue<HttpExchange>([Mock(HttpExchange), Mock(HttpExchange), Mock(HttpExchange)])

        def http1Client = Mock(HttpClient)
        http1Client.getDestinations() >> [http1Destination1, http1Destination2]

        def http2Client = Mock(HttpClient)
        http2Client.getDestinations() >> [http2Destination]

        def reporter = new HttpClientsWorkloadReporter(Mock(HermesMetrics), http1Client, new Http2ClientHolder(http2Client))

        expect:
        reporter.queuesSize == 6
        reporter.http1QueueSize == 3
        reporter.http2QueueSize == 3
    }

    def "should return sum of http/1 client destinations"() {
        given:
        def http1Destination1 = Mock(HttpDestination)
        http1Destination1.getHttpExchanges() >> new LinkedBlockingQueue<HttpExchange>([Mock(HttpExchange)])
        def http1Destination2 = Mock(HttpDestination)
        http1Destination2.getHttpExchanges() >> new LinkedBlockingQueue<HttpExchange>([Mock(HttpExchange), Mock(HttpExchange)])

        def http1Client = Mock(HttpClient)
        http1Client.getDestinations() >> [http1Destination1, http1Destination2]

        def reporter = new HttpClientsWorkloadReporter(Mock(HermesMetrics), http1Client, new Http2ClientHolder(null))

        expect:
        reporter.queuesSize == 3
        reporter.http1QueueSize == 3
        reporter.http2QueueSize == 0
    }
}
