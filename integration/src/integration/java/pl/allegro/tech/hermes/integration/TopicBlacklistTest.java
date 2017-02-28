package pl.allegro.tech.hermes.integration;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.BlacklistStatus;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.core.Response;
import java.util.Arrays;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class TopicBlacklistTest extends IntegrationTest {

    @Test
    public void shouldRefuseMessageOnBlacklistedTopic() {
        // given
        Topic topic = operations.buildTopic("group", "topic");
        TestMessage message = TestMessage.of("hello", "world");

        // when
        management.blacklist().blacklistTopics(Arrays.asList("group.topic"));
        wait.untilTopicBlacklisted("group.topic");

        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(FORBIDDEN);
    }

    @Test
    public void shouldAcceptMessageOnUnblacklistedTopic() {
        // given
        Topic topic = operations.buildTopic("group", "topic");
        TestMessage message = TestMessage.of("hello", "world");
        management.blacklist().blacklistTopics(Arrays.asList("group.topic"));
        wait.untilTopicBlacklisted("group.topic");

        // when
        management.blacklist().unblacklistTopic("group.topic");
        wait.untilTopicUnblacklisted("group.topic");

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
        operations.buildTopic("group", "topic");

        // when
        management.blacklist().blacklistTopics(Arrays.asList("group.topic"));
        wait.untilTopicBlacklisted("group.topic");

        // then
        assertThat(management.blacklist().isTopicBlacklisted("group.topic")).isEqualTo(BlacklistStatus.BLACKLISTED);

        // when
        management.blacklist().unblacklistTopic("group.topic");
        wait.untilTopicUnblacklisted("group.topic");

        // then
        assertThat(management.blacklist().isTopicBlacklisted("group.topic")).isEqualTo(BlacklistStatus.NOT_BLACKLISTED);
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
