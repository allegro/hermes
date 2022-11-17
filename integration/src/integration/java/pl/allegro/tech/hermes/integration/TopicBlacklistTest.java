package pl.allegro.tech.hermes.integration;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.util.Arrays;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static pl.allegro.tech.hermes.api.BlacklistStatus.BLACKLISTED;
import static pl.allegro.tech.hermes.api.BlacklistStatus.NOT_BLACKLISTED;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class TopicBlacklistTest extends IntegrationTest {

    @Test
    public void shouldRefuseMessageOnBlacklistedTopic() {
        // given
        Topic topic = operations.buildTopic(randomTopic("group", "topic").build());
        TestMessage message = TestMessage.of("hello", "world");

        // when
        management.blacklist().blacklistTopics(Arrays.asList(topic.getQualifiedName()));
        wait.untilTopicBlacklisted(topic.getQualifiedName());

        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(FORBIDDEN);
    }

    @Test
    public void shouldAcceptMessageOnUnblacklistedTopic() {
        // given
        Topic topic = operations.buildTopic(randomTopic("group", "topic").build());
        management.blacklist().blacklistTopics(Arrays.asList(topic.getQualifiedName()));
        wait.untilTopicBlacklisted(topic.getQualifiedName());
        TestMessage message = TestMessage.of("hello", "world");

        // when
        management.blacklist().unblacklistTopic(topic.getQualifiedName());
        wait.untilTopicUnblacklisted(topic.getQualifiedName());

        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(CREATED);
    }

    @Test
    public void shouldBlacklistNonExistingTopic() {
        // expect
        Response response = management.blacklist().blacklistTopics(Arrays.asList("nonExisting.topic"));
        wait.untilTopicBlacklisted("nonExisting.topic");

        // then
        assertThat(response).hasStatus(OK);
    }

    @Test
    public void shouldUnBlacklistTopic() {
        // given
        management.blacklist().blacklistTopics(Arrays.asList("group.topic"));
        wait.untilTopicBlacklisted("group.topic");

        // when
        Response response = management.blacklist().unblacklistTopic("group.topic");
        wait.untilTopicUnblacklisted("group.topic");

        // then
        assertThat(response).hasStatus(OK);
    }

    @Test
    public void shouldReportValidStatusOfTopic() {
        // given
        Topic topic = operations.buildTopic(randomTopic("group", "topic").build());

        // when
        management.blacklist().blacklistTopics(Arrays.asList(topic.getQualifiedName()));
        wait.untilTopicBlacklisted(topic.getQualifiedName());

        // then
        assertThat(management.blacklist().isTopicBlacklisted(topic.getQualifiedName())).isEqualTo(BLACKLISTED);

        // when
        management.blacklist().unblacklistTopic(topic.getQualifiedName());
        wait.untilTopicUnblacklisted(topic.getQualifiedName());

        // then
        assertThat(management.blacklist().isTopicBlacklisted(topic.getQualifiedName())).isEqualTo(NOT_BLACKLISTED);
    }

    @Test
    public void shouldReturnErrorOnNonBlacklistedUnblacklist() {
        // when
        Response response = management.blacklist().unblacklistTopic("group.topic");
        wait.untilTopicUnblacklisted("group.topic");

        // then
        assertThat(response).hasStatus(BAD_REQUEST);
    }

    @Test
    public void shouldProperlyReturnTopicsBlacklist() {
        // when
        management.blacklist().blacklistTopics(Arrays.asList("g.t1", "g.t2", "g.t3"));
        wait.untilTopicBlacklisted("g.t1");
        wait.untilTopicBlacklisted("g.t2");
        wait.untilTopicBlacklisted("g.t3");
        management.blacklist().unblacklistTopic("g.t2");
        wait.untilTopicUnblacklisted("group.topic");

        // then
        assertThat(management.blacklist().topicsBlacklist()).containsAll(Arrays.asList("g.t1", "g.t3"));
    }
}
