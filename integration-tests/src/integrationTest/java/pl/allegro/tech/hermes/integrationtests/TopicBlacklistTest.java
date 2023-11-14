package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.BlacklistStatus.BLACKLISTED;
import static pl.allegro.tech.hermes.api.BlacklistStatus.NOT_BLACKLISTED;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class TopicBlacklistTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @Test
    public void shouldRefuseMessageOnBlacklistedTopic() {
        // given
        Topic topic = hermes.api().createGroupAndTopic(topic("testGroup", "testTopic11").build());
        TestMessage message = TestMessage.of("hello", "world");

        // when
        hermes.api().blacklistTopic(topic.getQualifiedName());
        WebTestClient.ResponseSpec response = hermes.api().publish(topic.getQualifiedName(), message.body());

        // then
        response.expectStatus().isForbidden();
    }

    @Test
    public void shouldAcceptMessageOnUnblacklistedTopic() {
        // given
        Topic topic = hermes.api().createGroupAndTopic(topic("testGroup", "testTopic22").build());
        TestMessage message = TestMessage.of("hello", "world");
        hermes.api().blacklistTopic(topic.getQualifiedName());

        // when
        hermes.api().unblacklistTopic(topic.getQualifiedName());
        WebTestClient.ResponseSpec response = hermes.api().publish(topic.getQualifiedName(), message.body());

        // then
        response.expectStatus().is2xxSuccessful();
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
        hermes.api().blacklistTopic("group.topic");

        // when
        WebTestClient.ResponseSpec response = hermes.api().unblacklistTopicResponse("group.topic");

        // then
        response.expectStatus().isOk();
    }

    @Test
    public void shouldReportValidStatusOfTopic() {
        // given
        Topic topic = hermes.api().createGroupAndTopic(topic("testGroup", "testTopic33").build());

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
        Topic topic = hermes.api().createGroupAndTopic(topic("testGroup", "testTopic44").build());
        WebTestClient.ResponseSpec response = hermes.api().unblacklistTopicResponse(topic.getQualifiedName());

        // then
        response.expectStatus().isBadRequest();
    }
}
