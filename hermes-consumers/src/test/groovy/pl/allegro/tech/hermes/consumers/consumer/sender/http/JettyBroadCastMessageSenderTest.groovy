package pl.allegro.tech.hermes.consumers.consumer.sender.http

import com.github.tomakehurst.wiremock.WireMockServer
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.util.HttpCookieStore
import pl.allegro.tech.hermes.api.EndpointAddress
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult
import pl.allegro.tech.hermes.consumers.consumer.sender.MultiMessageSendingResult
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress
import pl.allegro.tech.hermes.test.helper.endpoint.MultiUrlEndpointAddressResolver
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.TimeUnit

import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.TEST_MESSAGE_CONTENT
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.testMessage

class JettyBroadCastMessageSenderTest extends Specification {

    @Shared
    List<Integer> ports = (1..4).collect { Ports.nextAvailable() }

    @Shared
    EndpointAddress endpoint = EndpointAddress.of(ports.collect {"http://localhost:${it}/"}.join(";"))

    @Shared
    HttpClient client;

    @Shared
    List<WireMockServer> wireMockServers = ports.collect { new WireMockServer(it) };

    @Shared
    List<RemoteServiceEndpoint> serviceEndpoints

    @Subject
    JettyBroadCastMessageSender messageSender

    def setupSpec() throws Exception {
        wireMockServers.forEach { it.start() }

        client = new HttpClient()
        client.setCookieStore(new HttpCookieStore.Empty())
        client.setConnectTimeout(1000)
        client.setIdleTimeout(1000)
        client.start()

        serviceEndpoints = wireMockServers.collect { new RemoteServiceEndpoint(it) }
    }

    def setup() {
        def address = new ResolvableEndpointAddress(endpoint, new MultiUrlEndpointAddressResolver());
        def httpRequestFactory = new HttpRequestFactory(client, 1000, new DefaultHttpMetadataAppender(), Optional.empty());
        messageSender = new JettyBroadCastMessageSender(httpRequestFactory, address);
    }

    def "should send message successfully in parallel to all urls"() {
        given:
        serviceEndpoints.forEach { endpoint -> endpoint.setDelay(300).expectMessages(TEST_MESSAGE_CONTENT)}

        when:
        def future = messageSender.send(testMessage());

        then:
        future.get(1, TimeUnit.SECONDS).succeeded()

        and:
        serviceEndpoints.forEach { it.waitUntilReceived() }
    }

    def "should return not succeeded when sending to one of urls fails"() {
        given:
        def failedServiceEndpoint = serviceEndpoints[0]
        failedServiceEndpoint.setReturnedStatusCode(500)
        serviceEndpoints.forEach { endpoint -> endpoint.expectMessages(TEST_MESSAGE_CONTENT)}

        when:
        def future = messageSender.send(testMessage())

        then:
        serviceEndpoints.forEach { it.waitUntilReceived() }

        and:
        MultiMessageSendingResult messageSendingResult = future.get(1, TimeUnit.SECONDS) as MultiMessageSendingResult
        !messageSendingResult.succeeded()

        and:
        messageSendingResult.children.find { it.statusCode == 500 && it.requestUri == failedServiceEndpoint.url }
    }

    def "should not send to already sent url on retry"() {
        given:
        serviceEndpoints.forEach { endpoint -> endpoint.expectMessages(TEST_MESSAGE_CONTENT) }
        def alreadySentServiceEndpoint = serviceEndpoints[0]

        Message message = testMessage();
        message.incrementRetryCounter([alreadySentServiceEndpoint.url]);

        when:
        def future = messageSender.send(message)

        then:
        alreadySentServiceEndpoint.makeSureNoneReceived()

        and:
        future.get(1, TimeUnit.SECONDS).succeeded()
    }

    def "should return not succeded and retry later when endpoint resolver return no hosts"() {
        given:
        def address = Stub(ResolvableEndpointAddress) {
            resolveAllFor(_) >> []
        }
        def httpRequestFactory = new HttpRequestFactory(client, 1000, new DefaultHttpMetadataAppender(), Optional.empty());
        messageSender = new JettyBroadCastMessageSender(httpRequestFactory, address);

        when:
        def future = messageSender.send(testMessage())

        then:
        MessageSendingResult messageSendingResult = future.get(1, TimeUnit.SECONDS)

        !messageSendingResult.succeeded()
        !messageSendingResult.isClientError()
        messageSendingResult.isRetryLater()

    }

    def cleanupSpec() {
        wireMockServers.forEach{ it.stop() }
        client.stop()
    }

}
