package pl.allegro.tech.hermes.tracker.mongo.frontend;

import com.codahale.metrics.MetricRegistry;
import com.github.fakemongo.Fongo;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.tracker.frontend.AbstractLogRepositoryTest;
import pl.allegro.tech.hermes.tracker.frontend.LogRepository;
import pl.allegro.tech.hermes.tracker.mongo.LogSchemaAware;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_SECOND;

public class MongoLogRepositoryTest extends AbstractLogRepositoryTest implements LogSchemaAware {

    private final DB database = new Fongo("trace").getDB("test");

    @Override
    protected LogRepository createRepository() {
        return new MongoLogRepository(database, 1000, 100, "cluster", "host", new MetricRegistry(), new PathsCompiler("localhost"));
    }

    @Override
    protected void awaitUntilMessageIsPersisted(String topic,
                                                String id,
                                                PublishedMessageTraceStatus status,
                                                String remoteHostname) throws Exception {
        awaitUntilMessageIsPersisted(
                new BasicDBObject(TOPIC_NAME, topic)
                .append(STATUS, status.toString())
                .append(MESSAGE_ID, id)
                .append(REMOTE_HOSTNAME, remoteHostname));
    }

    @Override
    protected void awaitUntilMessageIsPersisted(String topic,
                                                String id,
                                                PublishedMessageTraceStatus status,
                                                String reason,
                                                String remoteHostname) throws Exception {
        awaitUntilMessageIsPersisted(
                new BasicDBObject(TOPIC_NAME, topic)
                .append(STATUS, status.toString())
                .append(MESSAGE_ID, id)
                .append(REASON, reason)
                .append(REMOTE_HOSTNAME, remoteHostname));
    }

    private void awaitUntilMessageIsPersisted(DBObject query) throws Exception {
        await().atMost(ONE_SECOND).until(() -> {
            DBCursor cursor = database.getCollection(COLLECTION_PUBLISHED_NAME).find(query);
            return cursor.size() == 1;
        });
    }

}
