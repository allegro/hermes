package pl.allegro.tech.hermes.integration;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.env.FongoFactory;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class ExtraRequestHeadersLoggingTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldPublishAndConsumeMessage() {
        // given
        Topic topic = operations.buildTopic(randomTopic("publishAndConsumeGroup", "topic").build());
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        TestMessage message = TestMessage.of("hello", "world");
        remoteService.expectMessages(message.body());

        // when
        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(CREATED);
        remoteService.waitUntilReceived();
        getAllPublishedMessagesLogs();
    }

    private static void getAllPublishedMessagesLogs() {
        DB hermesDB = FongoFactory.hermesDB();
        DBCollection collection = hermesDB.getCollection("published_messages");
        DBCursor cursor = collection.find();
        System.out.println("rozmiar published_messages:");
        System.out.println(cursor.size());

        DBCollection collection2 = hermesDB.getCollection("sent_messages");
        DBCursor cursor2 = collection2.find();
        System.out.println("rozmiar sent_messages:");
        System.out.println(cursor2.size());

        assertThat(cursor.size()).isEqualTo(1);
    }
}
