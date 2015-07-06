package pl.allegro.tech.hermes.tracker.management

import com.github.fakemongo.Fongo
import com.mongodb.BasicDBObject
import com.mongodb.DB
import pl.allegro.tech.hermes.api.SentMessageTraceStatus
import pl.allegro.tech.hermes.tracker.mongo.LogSchemaAware
import pl.allegro.tech.hermes.tracker.mongo.management.MongoLogRepository
import spock.lang.Specification

class MongoLogRepositoryTest extends Specification implements LogSchemaAware {

    private DB database = new Fongo("Subscription fongo").getDB("hermesMessages");

    private MongoLogRepository repository = new MongoLogRepository(database)

    def "should ignore unknown fields when deserializing records"() {
        given:
        database.getCollection(COLLECTION_SENT_NAME)
                .insert(new BasicDBObject("foo", "bar")
                .append("topicName", "group.topic")
                .append("subscription", "subscription")
                .append("timestamp", 999912312L)
                .append("status", SentMessageTraceStatus.DISCARDED.toString()));

        when:
        List traces = repository.getLastUndeliveredMessages("group.topic", "subscription", 1)

        then:
        traces.size() == 1
        traces[0].topicName.qualifiedName() == "group.topic"
    }
}
