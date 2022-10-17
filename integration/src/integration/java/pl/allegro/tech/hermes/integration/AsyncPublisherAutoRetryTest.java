package pl.allegro.tech.hermes.integration;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.client.AsyncRestTemplate;
import pl.allegro.tech.hermes.client.HermesClient;
import pl.allegro.tech.hermes.client.HermesClientBuilder;
import pl.allegro.tech.hermes.client.restTemplate.RestTemplateHermesSender;
import pl.allegro.tech.hermes.integration.env.EnvironmentAware;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.apache.commons.lang3.StringUtils.strip;

@RunWith(MockitoJUnitRunner.class)
public class AsyncPublisherAutoRetryTest implements EnvironmentAware {
    static final String TOPICS_ROOT = "/topics/";
    static final String EXISTING_TOPIC = "existing";
    static final String EXISTING_TOPIC_URL = TOPICS_ROOT + EXISTING_TOPIC;
    static final String NOT_EXISTING_TOPIC = "not-existing";
    static final String NOT_EXISTING_TOPIC_URL = TOPICS_ROOT + NOT_EXISTING_TOPIC;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(FRONTEND_PORT);

    @Before
    public void before() {
        stubFor(post(urlEqualTo(NOT_EXISTING_TOPIC_URL)).willReturn(aResponse().withStatus(500)));
        stubFor(post(urlEqualTo(EXISTING_TOPIC_URL)).willReturn(aResponse().withStatus(200)));
    }

    @Test
    public void shouldRetryPublishingMessageEnoughTimesWhenServerReturns500() {
        //given
        int retries = 5;
        HermesClient hermesClient = retryingHermesClient(retries);

        //when
        hermesClient.publish(NOT_EXISTING_TOPIC, "{}").join();

        //then
        verify(retries + 1, postRequestedFor(urlEqualTo(NOT_EXISTING_TOPIC_URL)));
    }

    @Test
    public void shouldNotRetryIfMessageWasPublished() {
        HermesClient hermesClient = retryingHermesClient(10);

        //when
        hermesClient.publish(EXISTING_TOPIC, "{}").join();

        //then
        verify(1, postRequestedFor(urlEqualTo(EXISTING_TOPIC_URL)));
    }

    private HermesClient retryingHermesClient(int retries) {
         return HermesClientBuilder.hermesClient(new RestTemplateHermesSender(new AsyncRestTemplate()))
                .withRetries(retries)
                .withURI(URI.create(strip(FRONTEND_URL, "/"))).build();
    }

}
