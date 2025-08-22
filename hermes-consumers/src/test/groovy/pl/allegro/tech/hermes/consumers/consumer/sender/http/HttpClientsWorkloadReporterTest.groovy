package pl.allegro.tech.hermes.consumers.consumer.sender.http

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.transport.HttpDestination
import org.eclipse.jetty.client.transport.HttpExchange
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import spock.lang.Specification

import java.util.concurrent.LinkedBlockingQueue

class HttpClientsWorkloadReporterTest extends Specification {

    def "should return sum of http/1 and http/2 clients destinations"() {
        given:
        def http1Destination1 = Mock(HttpDestination)
        http1Destination1.getHttpExchanges() >> new LinkedBlockingQueue<HttpExchange>([Mock(HttpExchange)])
        def http1Destination2 = Mock(HttpDestination)
        http1Destination2.getHttpExchanges() >> new LinkedBlockingQueue<HttpExchange>([Mock(HttpExchange), Mock(HttpExchange)])

        def http1BatchDestination = Mock(HttpDestination)
        http1BatchDestination.getHttpExchanges() >> new LinkedBlockingQueue<HttpExchange>([Mock(HttpExchange), Mock(HttpExchange)])

        def http2Destination = Mock(HttpDestination)
        http2Destination.getHttpExchanges() >> new LinkedBlockingQueue<HttpExchange>([Mock(HttpExchange), Mock(HttpExchange), Mock(HttpExchange)])

        def http1Client = Mock(HttpClient)
        http1Client.getDestinations() >> [http1Destination1, http1Destination2]

        def http1BatchClient = Mock(HttpClient)
        http1BatchClient.getDestinations() >> [http1BatchDestination]

        def http2Client = Mock(HttpClient)
        http2Client.getDestinations() >> [http2Destination]

        def reporter = new HttpClientsWorkloadReporter(
                Mock(MetricsFacade),
                http1Client,
                http1BatchClient,
                new Http2ClientHolder(http2Client),
                true,
                false,
                false)

        expect:
        reporter.queuesSize == 8
        reporter.http1SerialQueueSize == 3
        reporter.http1BatchQueueSize == 2
        reporter.http2SerialQueueSize == 3
    }

    def "should return sum of http/1 serial client destinations"() {
        given:
        def http1Destination1 = Mock(HttpDestination)
        http1Destination1.getHttpExchanges() >> new LinkedBlockingQueue<HttpExchange>([Mock(HttpExchange)])
        def http1Destination2 = Mock(HttpDestination)
        http1Destination2.getHttpExchanges() >> new LinkedBlockingQueue<HttpExchange>([Mock(HttpExchange), Mock(HttpExchange)])

        def http1Client = Mock(HttpClient)
        http1Client.getDestinations() >> [http1Destination1, http1Destination2]

        def http1BatchClient = Mock(HttpClient)
        http1BatchClient.getDestinations() >> []

        def reporter = new HttpClientsWorkloadReporter(Mock(MetricsFacade), http1Client, http1BatchClient, new Http2ClientHolder(null), true, false, false)

        expect:
        reporter.queuesSize == 3
        reporter.http1SerialQueueSize == 3
        reporter.http1BatchQueueSize == 0
        reporter.http2SerialQueueSize == 0
    }
}
