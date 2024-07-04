package pl.allegro.tech.hermes.consumers.consumer.sender.http

import com.github.tomakehurst.wiremock.WireMockServer
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.http.HttpCookieStore
import pl.allegro.tech.hermes.api.EndpointAddress
import pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult
import pl.allegro.tech.hermes.consumers.consumer.sender.MultiMessageSendingResult
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.AuthHeadersProvider
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HermesHeadersProvider
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.Http1HeadersProvider
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout
import pl.allegro.tech.hermes.test.helper.endpoint.MultiUrlEndpointAddressResolver
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static java.util.Collections.singleton
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.TEST_MESSAGE_CONTENT
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.testMessage
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class JettyBroadCastMessageSenderTest extends Specification {

    @Shared
    List<Integer> ports = (1..4).collect { Ports.nextAvailable() }

    @Shared
    EndpointAddress endpoint = EndpointAddress.of(ports.collect { "http://localhost:${it}/" }.join(";"))

    @Shared
    HttpClient client

    @Shared
    List<WireMockServer> wireMockServers = ports.collect { new WireMockServer(it) }

    @Shared
    List<RemoteServiceEndpoint> serviceEndpoints

    HttpHeadersProvider requestHeadersProvider = new HermesHeadersProvider(
            singleton(new AuthHeadersProvider(new Http1HeadersProvider(), { Optional.empty() })))

    SendingResultHandlers resultHandlersProvider = new DefaultSendingResultHandlers()

    FutureAsyncTimeout futureAsyncTimeout = new FutureAsyncTimeout(Executors.newSingleThreadScheduledExecutor())

    def setupSpec() throws Exception {
        wireMockServers.forEach { it.start() }

        client = new HttpClient()
        client.setHttpCookieStore(new HttpCookieStore.Empty())
        client.setConnectTimeout(1000)
        client.setIdleTimeout(1000)
        client.start()

        serviceEndpoints = wireMockServers.collect { new RemoteServiceEndpoint(it) }
    }

    MessageSender getSender(ConsumerRateLimiter rateLimiter) {
        def address = new ResolvableEndpointAddress(endpoint, new MultiUrlEndpointAddressResolver(),
                EndpointAddressResolverMetadata.empty())
        def httpRequestFactory = new DefaultHttpRequestFactory(client, 1000, 1000, new DefaultHttpMetadataAppender())

        Subscription subscription = subscription(SubscriptionName.fromString("group.topic\$subscription")).build()

        ResilientMessageSender sendFutureProvider = new ResilientMessageSender(
                rateLimiter, subscription, futureAsyncTimeout, 10000, 1000
        )

        return new JettyBroadCastMessageSender(httpRequestFactory, address,
                requestHeadersProvider, resultHandlersProvider, sendFutureProvider)
    }

    def "should send message successfully in parallel to all urls"() {
        given:
        ConsumerRateLimiter rateLimiter = Mock(ConsumerRateLimiter) {
            4 * registerSuccessfulSending()
        }

        serviceEndpoints.forEach { endpoint -> endpoint.setDelay(300).expectMessages(TEST_MESSAGE_CONTENT) }

        when:
        def future = getSender(rateLimiter).send(testMessage())

        then:
        future.get(10, TimeUnit.SECONDS).succeeded()


        and:
        serviceEndpoints.forEach { it.waitUntilReceived() }
    }

    def "should return not succeeded when sending to one of urls fails"() {
        given:
        ConsumerRateLimiter rateLimiter = Mock(ConsumerRateLimiter) {
            3 * registerSuccessfulSending()
            1 * registerFailedSending()
        }

        def failedServiceEndpoint = serviceEndpoints[0]
        failedServiceEndpoint.setReturnedStatusCode(500)
        serviceEndpoints.forEach { endpoint -> endpoint.expectMessages(TEST_MESSAGE_CONTENT) }

        when:
        def future = getSender(rateLimiter).send(testMessage())

        then:
        serviceEndpoints.forEach { it.waitUntilReceived() }

        and:
        MultiMessageSendingResult messageSendingResult = future.get(1, TimeUnit.SECONDS) as MultiMessageSendingResult
        !messageSendingResult.succeeded()

        and:
        messageSendingResult.children.find { it.statusCode == 500 && it.requestUri.get() == failedServiceEndpoint.url }
    }

    def "should not send to already sent url on retry"() {
        given:
        ConsumerRateLimiter rateLimiter = Mock(ConsumerRateLimiter) {
            3 * registerSuccessfulSending()
        }

        serviceEndpoints.forEach { endpoint -> endpoint.expectMessages(TEST_MESSAGE_CONTENT) }
        def alreadySentServiceEndpoint = serviceEndpoints[0]

        Message message = testMessage()
        message.incrementRetryCounter([alreadySentServiceEndpoint.url])

        when:
        def future = getSender(rateLimiter).send(message)

        then:
        alreadySentServiceEndpoint.makeSureNoneReceived()

        and:
        future.get(1, TimeUnit.SECONDS).succeeded()
    }

    def "should return not succeeded and retry later when endpoint resolver return no hosts and no message was sent previously"() {
        given:
        def address = Stub(ResolvableEndpointAddress) {
            resolveAllFor(_ as Message) >> []

            getRawAddress() >> endpoint
        }

        def httpRequestFactory = new DefaultHttpRequestFactory(client, 1000, 1000, new DefaultHttpMetadataAppender())
        MessageSender messageSender = new JettyBroadCastMessageSender(httpRequestFactory, address,
                requestHeadersProvider, resultHandlersProvider, Mock(ResilientMessageSender))

        when:
        def future =  messageSender.send(testMessage())

        then:
        MessageSendingResult messageSendingResult = future.get(1, TimeUnit.SECONDS)

        !messageSendingResult.succeeded()
        !messageSendingResult.isClientError()
        messageSendingResult.isRetryLater()
    }

    def "should return succeeded when endpoint resolver return no hosts and but message was sent previously"() {
        given:
        Message message = testMessage()
        message.incrementRetryCounter([serviceEndpoints[0].url])
        def address = Stub(ResolvableEndpointAddress) {
            resolveAllFor(_ as Message) >> []

            getRawAddress() >> endpoint
        }

        def httpRequestFactory = new DefaultHttpRequestFactory(client, 1000, 1000, new DefaultHttpMetadataAppender())
        MessageSender messageSender = new JettyBroadCastMessageSender(httpRequestFactory, address,
                requestHeadersProvider, resultHandlersProvider, Mock(ResilientMessageSender))

        when:
        def future =  messageSender.send(message)

        then:
        MessageSendingResult messageSendingResult = future.get(1, TimeUnit.SECONDS)

        messageSendingResult.succeeded()
    }


    def "should return succeeded when endpoint resolver returns the same urls that the message was already sent to"() {
        given: "a message that was sent"
        ConsumerRateLimiter rateLimiter = Mock(ConsumerRateLimiter) {
            0 * registerSuccessfulSending()
        }

        serviceEndpoints.forEach { endpoint -> endpoint.expectMessages(TEST_MESSAGE_CONTENT) }

        Message message = testMessage()
        message.incrementRetryCounter(serviceEndpoints.collect { it.url })

        when:
        def future = getSender(rateLimiter).send(message)

        then:
        MessageSendingResult messageSendingResult = future.get(1, TimeUnit.SECONDS)
        messageSendingResult.succeeded()
    }

    def cleanupSpec() {
        wireMockServers.forEach { it.stop() }
        client.stop()
    }

}
