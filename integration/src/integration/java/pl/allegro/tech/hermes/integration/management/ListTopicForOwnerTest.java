package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ListTopicForOwnerTest extends IntegrationTest {

    @Test
    public void shouldListTopicsForOwnerId() {
        // given
        createTopicForOwner("groupOwnerId1a", "topic", "ownerIdTest1");
        createTopicForOwner("groupOwnerId1b", "topic", "ownerIdTest1");
        createTopicForOwner("groupOwnerId2", "topic", "ownerIdTest2");

        // expect
        assertThat(listTopicsForOwner("ownerIdTest1")).containsExactly("groupOwnerId1a.topic", "groupOwnerId1b.topic");
        assertThat(listTopicsForOwner("ownerIdTest2")).containsExactly("groupOwnerId2.topic");
    }

    @Test
    public void shouldListTopicAfterNewTopicIsAdded() {
        // expect empty list on start
        assertThat(listTopicsForOwner("ownerIdTest3")).isEmpty();

        // when
        createTopicForOwner("groupOwnerId3", "topic", "ownerIdTest3");

        // then
        assertThat(listTopicsForOwner("ownerIdTest3")).containsExactly("groupOwnerId3.topic");
    }

    @Test
    public void shouldListTopicAfterOwnerIsChanged() {
        // given
        createTopicForOwner("groupOwnerId4", "topic", "ownerIdTest4");

        // then
        assertThat(listTopicsForOwner("ownerIdTest4")).containsExactly("groupOwnerId4.topic");
        assertThat(listTopicsForOwner("ownerIdTest5")).isEmpty();

        // when
        operations.updateTopic("groupOwnerId4", "topic", PatchData.patchData()
                .set("owner", new OwnerId("Plaintext", "ownerIdTest5"))
                .build());

        // then
        assertThat(listTopicsForOwner("ownerIdTest4")).isEmpty();
        assertThat(listTopicsForOwner("ownerIdTest5")).containsExactly("groupOwnerId4.topic");
    }

    @Test
    public void shouldNotListTopicAfterTopicIsDeleted() {
        // given
        createTopicForOwner("groupOwnerId6", "topic", "ownerIdTest6");
        assertThat(listTopicsForOwner("ownerIdTest6")).containsExactly("groupOwnerId6.topic");

        // when
        management.topic().remove("groupOwnerId6.topic");

        // then
        assertThat(listTopicsForOwner("ownerIdTest6")).isEmpty();
    }

    private void createTopicForOwner(String group, String topic, String ownerId) {
        operations.createGroup(group);
        operations.createTopic(TopicBuilder.topic(group, topic)
                .withOwner(new OwnerId("Plaintext", ownerId))
                .build());
    }

    private List<String> listTopicsForOwner(String ownerId) {
        return management.topic().listForOwner("Plaintext", ownerId)
                .stream()
                .map(Topic::getQualifiedName)
                .collect(Collectors.toList());
    }
}
