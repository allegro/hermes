package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.api.BlacklistStatus.BLACKLISTED;
import static pl.allegro.tech.hermes.api.BlacklistStatus.NOT_BLACKLISTED;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class TopicBlacklistTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @Test
    public void shouldRefuseMessageOnBlacklistedTopic() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestMessage message = TestMessage.of("hello", "world");

        // when
        hermes.api().blacklistTopic(topic.getQualifiedName());
        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            WebTestClient.ResponseSpec response = hermes.api().publish(topic.getQualifiedName(), message.body());

            // then
            response.expectStatus().isForbidden();
        });
    }

    @Test
    public void shouldAcceptMessageOnUnblacklistedTopic() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestMessage message = TestMessage.of("hello", "world");
        hermes.api().blacklistTopic(topic.getQualifiedName());

        // when
        hermes.api().unblacklistTopic(topic.getQualifiedName());
        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            WebTestClient.ResponseSpec response = hermes.api().publish(topic.getQualifiedName(), message.body());

            // then
            response.expectStatus().is2xxSuccessful();
        });
    }

    @Test
    public void shouldBlacklistNonExistingTopic() {
        // expect
        WebTestClient.ResponseSpec response = hermes.api().blacklistTopicResponse("nonExisting.topic");

        // then
        response.expectStatus().isOk();
    }


    @Test
    public void shouldUnBlacklistTopic() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        hermes.api().blacklistTopic(topic.getQualifiedName());

        // when
        WebTestClient.ResponseSpec response = hermes.api().unblacklistTopicResponse(topic.getQualifiedName());

        // then
        response.expectStatus().isOk();
    }

    @Test
    public void shouldReportValidStatusOfTopic() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        // when
        hermes.api().blacklistTopic(topic.getQualifiedName());

        // then
        assertThat(hermes.api().isTopicBlacklisted(topic.getQualifiedName())).isEqualTo(BLACKLISTED);

        // when
        hermes.api().unblacklistTopic(topic.getQualifiedName());

        // then
        assertThat(hermes.api().isTopicBlacklisted(topic.getQualifiedName())).isEqualTo(NOT_BLACKLISTED);
    }

    @Test
    public void shouldReturnErrorOnNonBlacklistedUnblacklist() {
        // when
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        WebTestClient.ResponseSpec response = hermes.api().unblacklistTopicResponse(topic.getQualifiedName());

        // then
        response.expectStatus().isBadRequest();
    }
}