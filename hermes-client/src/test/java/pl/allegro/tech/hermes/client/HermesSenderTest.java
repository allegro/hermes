package pl.allegro.tech.hermes.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.squareup.okhttp.OkHttpClient;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.web.client.AsyncRestTemplate;
import pl.allegro.tech.hermes.client.jersey.JerseyHermesSender;
import pl.allegro.tech.hermes.client.okhttp.OkHttpHermesSender;
import pl.allegro.tech.hermes.client.restTemplate.RestTemplateHermesSender;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.client.ClientBuilder;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class HermesSenderTest {

    @ClassRule
    public static WireMockClassRule service = new WireMockClassRule(Ports.nextAvailable());

    @Before
    public void setUp() {
        WireMock.reset();
    }

    @Test
    public void shouldSendContentTypeHeaderWhenPublishingUsingJersey() {
        // given
        HermesClient client = HermesClientBuilder
                .hermesClient(new JerseyHermesSender(ClientBuilder.newClient()))
                .withDefaultContentType("application/json")
                .withURI(URI.create("http://localhost:" + service.port()))
                .build();

        // when
        client.publish("topic.jersey", "Hello!").join();

        // then
        service.verify(postRequestedFor(urlEqualTo("/topics/topic.jersey"))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void shouldSendContentTypeHeaderWhenPublishingUsingRestTemplate() {
        // given
        HermesClient client = HermesClientBuilder
                .hermesClient(new RestTemplateHermesSender(new AsyncRestTemplate()))
                .withDefaultContentType("application/json")
                .withURI(URI.create("http://localhost:" + service.port()))
                .build();

        // when
        client.publish("topic.spring", "Hello!").join();

        // then
        service.verify(postRequestedFor(urlEqualTo("/topics/topic.spring"))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void shouldSendContentTypeHeaderWhenPublishingUsingOkHttp() {
        // given
        HermesClient client = HermesClientBuilder
                .hermesClient(new OkHttpHermesSender(new OkHttpClient()))
                .withDefaultContentType("application/json")
                .withURI(URI.create("http://localhost:" + service.port()))
                .build();

        // when
        client.publish("topic.okhttp", "Hello!").join();

        // then - OkHttp always appends charset information
        service.verify(postRequestedFor(urlEqualTo("/topics/topic.okhttp"))
                .withHeader("Content-Type", equalTo("application/json; charset=utf-8")));
    }
}
