package pl.allegro.tech.hermes.integration;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import pl.allegro.tech.hermes.client.HermesClient;
import pl.allegro.tech.hermes.client.HermesClientBuilder;
import pl.allegro.tech.hermes.client.HermesSender;
import pl.allegro.tech.hermes.client.restTemplate.RestTemplateHermesSender;
import pl.allegro.tech.hermes.client.webclient.WebClientHermesSender;
import pl.allegro.tech.hermes.integration.env.EnvironmentAware;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
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
    public void shouldRetryPublishingMessageEnoughTimesWhenServerReturns500UsingAsyncRestTemplate() {
        //given
        HermesSender sender = new RestTemplateHermesSender(new AsyncRestTemplate());
        int retries = 5;
        HermesClient hermesClient = retryingHermesClient(retries, sender);

        //when
        hermesClient.publish(NOT_EXISTING_TOPIC, "{}").join();

        //then
        verify(retries + 1, postRequestedFor(urlEqualTo(NOT_EXISTING_TOPIC_URL)));
    }

    @Test
    public void shouldNotRetryIfMessageWasPublishedUsingAsyncRestTemplate() {
        HermesSender sender = new RestTemplateHermesSender(new AsyncRestTemplate());
        HermesClient hermesClient = retryingHermesClient(10, sender);

        //when
        hermesClient.publish(EXISTING_TOPIC, "{}").join();

        //then
        verify(1, postRequestedFor(urlEqualTo(EXISTING_TOPIC_URL)));
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
        verify(retries + 1, postRequestedFor(urlEqualTo(NOT_EXISTING_TOPIC_URL)));
    }

    @Test
    public void shouldNotRetryIfMessageWasPublishedUsingWebClient() {
        HermesSender sender = new WebClientHermesSender(WebClient.create());
        HermesClient hermesClient = retryingHermesClient(10, sender);

        //when
        hermesClient.publish(EXISTING_TOPIC, "{}").join();

        //then
        verify(1, postRequestedFor(urlEqualTo(EXISTING_TOPIC_URL)));
    }

    private HermesClient retryingHermesClient(int retries, HermesSender sender) {
         return HermesClientBuilder.hermesClient(sender)
                .withRetries(retries)
                .withURI(URI.create(strip(FRONTEND_URL, "/"))).build();
    }

}
