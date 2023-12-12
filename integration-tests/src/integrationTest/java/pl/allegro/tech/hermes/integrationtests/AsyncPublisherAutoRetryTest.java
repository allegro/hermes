package pl.allegro.tech.hermes.integrationtests;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import pl.allegro.tech.hermes.client.HermesClient;
import pl.allegro.tech.hermes.client.HermesClientBuilder;
import pl.allegro.tech.hermes.client.HermesSender;
import pl.allegro.tech.hermes.client.webclient.WebClientHermesSender;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class AsyncPublisherAutoRetryTest {
    private static final String TOPICS_ROOT = "/topics/";
    private static final String EXISTING_TOPIC = "existing";
    private static final String EXISTING_TOPIC_URL = TOPICS_ROOT + EXISTING_TOPIC;
    private static final String NOT_EXISTING_TOPIC = "not-existing";
    private static final String NOT_EXISTING_TOPIC_URL = TOPICS_ROOT + NOT_EXISTING_TOPIC;
    private static final int frontendPort = Ports.nextAvailable();
    private static final WireMockServer frontendMock = new WireMockServer(frontendPort);

    @BeforeAll
    public static void setup() {
        frontendMock.addStubMapping(post(urlEqualTo(NOT_EXISTING_TOPIC_URL)).willReturn(aResponse().withStatus(500)).build());
        frontendMock.addStubMapping(post(urlEqualTo(EXISTING_TOPIC_URL)).willReturn(aResponse().withStatus(200)).build());
        frontendMock.start();
    }

    @AfterAll
    public static void cleanup() {
        frontendMock.stop();
    }

    @Test
    public void shouldRetryPublishingMessageEnoughTimesWhenServerReturns500UsingWebClient() {
        //given
        HermesSender sender = new WebClientHermesSender(WebClient.create());
        int retries = 5;
        HermesClient hermesClient = retryingHermesClient(retries, sender);

        //when
        hermesClient.publish(NOT_EXISTING_TOPIC, "{}").join();

        //then
        frontendMock.verify(retries + 1,  postRequestedFor(urlEqualTo(NOT_EXISTING_TOPIC_URL)));
    }

    @Test
    public void shouldNotRetryIfMessageWasPublishedUsingWebClient() {
        HermesSender sender = new WebClientHermesSender(WebClient.create());
        HermesClient hermesClient = retryingHermesClient(10, sender);

        //when
        hermesClient.publish(EXISTING_TOPIC, "{}").join();

        //then
        frontendMock.verify(1, postRequestedFor(urlEqualTo(EXISTING_TOPIC_URL)));
    }

    private HermesClient retryingHermesClient(int retries, HermesSender sender) {
        return HermesClientBuilder.hermesClient(sender)
                .withRetries(retries)
                .withURI(URI.create("http://localhost:" + frontendPort)).build();
    }

}
