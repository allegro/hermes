package pl.allegro.tech.hermes.integrationtests.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class ListTopicForOwnerTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @Test
    public void shouldListTopicsForOwnerId() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().withOwner(new OwnerId("Plaintext", "Team A")).build());
        Topic topic2 = hermes.initHelper().createTopic(topicWithRandomName().withOwner(new OwnerId("Plaintext", "Team A")).build());
        Topic topic3 = hermes.initHelper().createTopic(topicWithRandomName().withOwner(new OwnerId("Plaintext", "Team B")).build());

        // expect
        assertThat(listTopicsForOwner("Team A")).containsExactly(topic.getQualifiedName(), topic2.getQualifiedName());
        assertThat(listTopicsForOwner("Team B")).containsExactly(topic3.getQualifiedName());
    }

    @Test
    public void shouldListTopicAfterNewTopicIsAdded() {
        // expect empty list on start
        assertThat(listTopicsForOwner("Team C")).isEmpty();

        // when
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().withOwner(new OwnerId("Plaintext", "Team C")).build());

        // then
        assertThat(listTopicsForOwner("Team C")).containsExactly(topic.getQualifiedName());
    }

    @Test
    public void shouldListTopicAfterOwnerIsChanged() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().withOwner(new OwnerId("Plaintext", "Team D")).build());

        // then
        assertThat(listTopicsForOwner("Team D")).containsExactly(topic.getQualifiedName());
        assertThat(listTopicsForOwner("Team E")).isEmpty();

        // when
        hermes.api().updateTopic(topic.getQualifiedName(),
                PatchData.patchData().set("owner", new OwnerId("Plaintext", "Team E")).build());

        // then
        assertThat(listTopicsForOwner("Team D")).isEmpty();
        assertThat(listTopicsForOwner("Team E")).containsExactly(topic.getQualifiedName());
    }

    @Test
    public void shouldNotListTopicAfterTopicIsDeleted() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().withOwner(new OwnerId("Plaintext", "Team F")).build());
        assertThat(listTopicsForOwner("Team F")).containsExactly(topic.getQualifiedName());

        // when
        hermes.api().deleteTopic(topic.getQualifiedName()).expectStatus().is2xxSuccessful();

        // then
        assertThat(listTopicsForOwner("Team F")).isEmpty();
    }

    private List<String> listTopicsForOwner(String ownerId) {
        return Objects.requireNonNull(hermes.api().getTopicsForOwner("Plaintext", ownerId)
                        .expectStatus()
                        .is2xxSuccessful()
                        .expectBodyList(Topic.class)
                        .returnResult()
                        .getResponseBody())
                .stream()
                .map(Topic::getQualifiedName)
                .collect(Collectors.toList());
    }
}

