package pl.allegro.tech.hermes.integration.management;

import jakarta.ws.rs.core.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.integration.IntegrationTest;

import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class TopicManagementTest extends IntegrationTest {

    @Test
    public void shouldCreateTopicEvenIfExistsInBrokers() {
        // given
        String groupName = "existingTopicFromExternalBroker";
        String topicName = "topic";
        String qualifiedTopicName = groupName + "." + topicName;

        brokerOperations.createTopic(qualifiedTopicName);
        operations.createGroup(groupName);

        // when
        Response response = management.topic().create(topicWithSchema(topic(groupName, topicName).build()));

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        Assertions.assertThat(management.topic().get(qualifiedTopicName)).isNotNull();
    }

    @Test
    public void topicCreationRollbackShouldNotDeleteTopicOnBroker() throws Throwable {
        // given
        String groupName = "topicCreationRollbackShouldNotDeleteTopicOnBroker";
        String topicName = "topic";
        String qualifiedTopicName = groupName + "." + topicName;

        brokerOperations.createTopic(qualifiedTopicName, PRIMARY_KAFKA_CLUSTER_NAME);
        operations.createGroup(groupName);

        // when
        management.topic().create(topicWithSchema(topic(groupName, topicName).build()));

        // then
        assertThat(brokerOperations.topicExists(qualifiedTopicName, PRIMARY_KAFKA_CLUSTER_NAME)).isTrue();
        assertThat(brokerOperations.topicExists(qualifiedTopicName, SECONDARY_KAFKA_CLUSTER_NAME)).isFalse();
    }
}
