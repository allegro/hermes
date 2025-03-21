package pl.allegro.tech.hermes.client

import com.github.tomakehurst.wiremock.junit.WireMockClassRule
import jakarta.ws.rs.client.ClientBuilder
import okhttp3.OkHttpClient
import org.junit.ClassRule
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import pl.allegro.tech.hermes.client.jersey.JerseyHermesSender
import pl.allegro.tech.hermes.client.okhttp.OkHttpHermesSender
import pl.allegro.tech.hermes.client.restclient.RestClientHermesSender
import pl.allegro.tech.hermes.client.webclient.WebClientHermesSender
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static pl.allegro.tech.hermes.client.HermesMessage.hermesMessage

class HermesSenderTest extends Specification {

    @ClassRule
    @Shared
    WireMockClassRule service = new WireMockClassRule(14523)

    void setup() {
        reset()
    }

    @Unroll
    def "should send Content-Type header when publishing using #name sender"() {
        given:
        HermesSender currentSender = sender
        HermesClient client = HermesClientBuilder
                .hermesClient(currentSender)
                .withDefaultContentType("application/json")
                .withURI(URI.create("http://localhost:" + service.port()))
                .build()

        service.stubFor(post(urlEqualTo('/topics/topic.test'))
                .willReturn(aResponse().withStatus(201)))

        when:
        client.publish("topic.test", "Hello!").join()

        then:
        service.verify(postRequestedFor(urlEqualTo("/topics/topic.test"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(containing("Hello!")))

        where:
        sender                                            | name
        new JerseyHermesSender(ClientBuilder.newClient()) | 'JerseySender'
        new OkHttpHermesSender(new OkHttpClient())        | 'OkHttpSender'
        new WebClientHermesSender(WebClient.create())     | 'WebClientSender'
        new RestClientHermesSender(RestClient.create())   | 'RestClient'
    }

    @Unroll
    def "should send Schema-Version header when publishing with schema using #name sender"() {
        given:
        HermesSender currentSender = sender
        HermesClient client = HermesClientBuilder
                .hermesClient(currentSender)
                .withURI(URI.create("http://localhost:" + service.port()))
                .build()

        service.stubFor(post(urlEqualTo('/topics/topic.test'))
                .willReturn(aResponse().withStatus(201)))

        when:
        client.publish(hermesMessage('topic.test', 'Hello!').withSchemaVersion(13).build()).join()

        then:
        service.verify(postRequestedFor(urlEqualTo("/topics/topic.test"))
                .withHeader("Schema-Version", equalTo("13"))
                .withRequestBody(containing("Hello!")))

        where:
        sender                                            | name
        new JerseyHermesSender(ClientBuilder.newClient()) | 'JerseySender'
        new OkHttpHermesSender(new OkHttpClient())        | 'OkHttpSender'
        new WebClientHermesSender(WebClient.create())     | 'WebClientSender'
        new RestClientHermesSender(RestClient.create())   | 'RestClient'
    }

    @Unroll
    def "should send custom header when publishing using #name sender"() {
        given:
        HermesSender currentSender = sender
        HermesClient client = HermesClientBuilder
                .hermesClient(currentSender)
                .withURI(URI.create("http://localhost:" + service.port()))
                .build()

        service.stubFor(post(urlEqualTo('/topics/topic.test'))
                .willReturn(aResponse().withStatus(201)))

        when:
        client.publish(hermesMessage('topic.test', 'Hello!').withHeader('Custom-Header', 'header value').build()).join()

        then:
        service.verify(postRequestedFor(urlEqualTo("/topics/topic.test"))
                .withHeader("Custom-Header", equalTo("header value"))
                .withRequestBody(containing("Hello!")))

        where:
        sender                                            | name
        new JerseyHermesSender(ClientBuilder.newClient()) | 'JerseySender'
        new OkHttpHermesSender(new OkHttpClient())        | 'OkHttpSender'
        new WebClientHermesSender(WebClient.create())     | 'WebClientSender'
        new RestClientHermesSender(RestClient.create())   | 'RestClient'
    }

    @Unroll
    def "should read header #header being case insensitive when publishing using #name sender"() {
        given:
        HermesSender currentSender = sender
        HermesClient client = HermesClientBuilder
                .hermesClient(currentSender)
                .withDefaultContentType("application/json")
                .withURI(URI.create("http://localhost:" + service.port()))
                .build()

        service.stubFor(
                post(urlEqualTo('/topics/topic.test'))
                        .willReturn(aResponse()
                                .withStatus(201)
                                .withHeader(header, 'messageId'))
        )

        when:
        def response = client.publish("topic.test", "Hello!").join()

        then:
        service.verify(postRequestedFor(urlEqualTo("/topics/topic.test"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(containing("Hello!")))

        and:
        response.messageId == 'messageId'

        where:
        sender                                            | name              | header
        new JerseyHermesSender(ClientBuilder.newClient()) | 'JerseySender'    | 'Hermes-Message-Id'
        new OkHttpHermesSender(new OkHttpClient())        | 'OkHttpSender'    | 'Hermes-Message-Id'
        new WebClientHermesSender(WebClient.create())     | 'WebClientSender' | 'Hermes-Message-Id'
        new RestClientHermesSender(RestClient.create())   | 'RestClient'      | 'Hermes-Message-Id'


        new JerseyHermesSender(ClientBuilder.newClient()) | 'JerseySender'    | 'hermes-message-id'
        new OkHttpHermesSender(new OkHttpClient())        | 'OkHttpSender'    | 'hermes-message-id'
        new WebClientHermesSender(WebClient.create())     | 'WebClientSender' | 'hermes-message-id'
        new RestClientHermesSender(RestClient.create())   | 'RestClient'      | 'hermes-message-id'


        new JerseyHermesSender(ClientBuilder.newClient()) | 'JerseySender'    | 'HERMES-MESSAGE-ID'
        new OkHttpHermesSender(new OkHttpClient())        | 'OkHttpSender'    | 'HERMES-MESSAGE-ID'
        new WebClientHermesSender(WebClient.create())     | 'WebClientSender' | 'HERMES-MESSAGE-ID'
        new RestClientHermesSender(RestClient.create())   | 'RestClient'      | 'HERMES-MESSAGE-ID'

    }
}
