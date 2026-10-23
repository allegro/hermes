package pl.allegro.tech.hermes.consumers.consumer.oauth.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.Request
import org.eclipse.jetty.http.HttpMethod
import spock.lang.Specification

class OAuthHttpClientTest extends Specification {

    def "restores thread interrupt status when token request is interrupted"() {
        given:
        def httpClient = Mock(HttpClient)
        def httpRequest = Mock(Request)
        def request = OAuthTokenRequest.oAuthTokenRequest()
                .withUrl("https://oauth.example.test/token")
                .withRequestTimeout(1000)
                .withSocketTimeout(1000)
                .build()
        def client = new OAuthHttpClient(httpClient, new ObjectMapper())

        httpClient.newRequest(request.url) >> httpRequest
        httpRequest.timeout(_, _) >> httpRequest
        httpRequest.idleTimeout(_, _) >> httpRequest
        httpRequest.method(HttpMethod.POST) >> httpRequest
        httpRequest.headers(_) >> httpRequest
        httpRequest.send() >> { throw new InterruptedException("synthetic interruption") }

        when:
        client.getToken(request)

        then:
        def exception = thrown(OAuthTokenRequestException)
        exception.cause instanceof InterruptedException
        Thread.currentThread().isInterrupted()

        cleanup:
        Thread.interrupted()
    }
}
