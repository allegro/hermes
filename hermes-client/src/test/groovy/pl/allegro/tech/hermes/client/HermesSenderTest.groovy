package pl.allegro.tech.hermes.client

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockClassRule
import com.squareup.okhttp.OkHttpClient
import org.junit.ClassRule
import org.springframework.web.client.AsyncRestTemplate
import pl.allegro.tech.hermes.client.jersey.JerseyHermesSender
import pl.allegro.tech.hermes.client.okhttp.OkHttpHermesSender
import pl.allegro.tech.hermes.client.restTemplate.RestTemplateHermesSender
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.client.ClientBuilder

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class HermesSenderTest extends Specification {

    @ClassRule
    @Shared
    WireMockClassRule service = new WireMockClassRule(14523);

    void setup() {
        WireMock.reset();
    }

    @Unroll("should send Content-Type header when publishing using #name sender")
    def "should send Content-Type header"() {
        given:
        HermesSender currentSender = sender
        HermesClient client = HermesClientBuilder
                .hermesClient(currentSender)
                .withDefaultContentType("application/json")
                .withURI(URI.create("http://localhost:" + service.port()))
                .build()

        when:
        client.publish("topic.jersey", "Hello!").join()

        then:
        service.verify(postRequestedFor(urlEqualTo("/topics/topic.jersey"))
                .withHeader("Content-Type", equalTo("application/json")))

        where:
        name << ['JerseySender', 'RestTemplateSender', 'OkHttpSender']
        sender << [
                new JerseyHermesSender(ClientBuilder.newClient()),
                new RestTemplateHermesSender(new AsyncRestTemplate()),
                new OkHttpHermesSender(new OkHttpClient())
        ]
    }

    @Unroll("should send Schema-Version header when publishing with schema using #name sender")
    def "should send Schema-Version header"() {
        given:
        HermesSender currentSender = sender
        HermesClient client = HermesClientBuilder
                .hermesClient(currentSender)
                .withURI(URI.create("http://localhost:" + service.port()))
                .build()

        when:
        client.publishAvro("topic.jersey", 13, "Hello!".bytes).join()

        then:
        service.verify(postRequestedFor(urlEqualTo("/topics/topic.jersey"))
                .withHeader("Schema-Version", equalTo("13")))

        where:
        name << ['JerseySender', 'RestTemplateSender', 'OkHttpSender']
        sender << [
                new JerseyHermesSender(ClientBuilder.newClient()),
                new RestTemplateHermesSender(new AsyncRestTemplate()),
                new OkHttpHermesSender(new OkHttpClient())
        ]
    }

}
