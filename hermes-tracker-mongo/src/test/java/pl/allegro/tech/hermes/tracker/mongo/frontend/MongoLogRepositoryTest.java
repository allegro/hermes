package pl.allegro.tech.hermes.tracker.mongo.frontend;

import com.codahale.metrics.MetricRegistry;
import com.github.fakemongo.Fongo;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.tracker.frontend.AbstractLogRepositoryTest;
import pl.allegro.tech.hermes.tracker.frontend.LogRepository;
import pl.allegro.tech.hermes.tracker.mongo.LogSchemaAware;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_SECOND;
import static com.mongodb.QueryBuilder.start;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;

public class MongoLogRepositoryTest extends AbstractLogRepositoryTest implements LogSchemaAware {

    private final DB database = new Fongo("trace").getDB("test");

    @Override
    protected LogRepository createRepository() {
        return new MongoLogRepository(database, 1000, 100, "cluster", "host", new MetricRegistry(), new PathsCompiler("localhost"));
    }

    @Override
    protected void awaitUntilMessageIsPersisted(String topic,
                                                String id,
                                                String remoteHostname,
                                                PublishedMessageTraceStatus status,
                                                String... extraRequestHeadersKeywords) throws Exception {
        awaitUntilMessageIsPersisted(constructExpectedLogQuery(topic, id, "host", status, extraRequestHeadersKeywords));
    }

    @Override
    protected void awaitUntilMessageIsPersisted(String topic,
                                                String id,
                                                String reason,
                                                String remoteHostname,
                                                PublishedMessageTraceStatus status,
                                                String... extraRequestHeadersKeywords) throws Exception {
        awaitUntilMessageIsPersisted(constructExpectedLogQuery(topic, id, "host", reason, status, extraRequestHeadersKeywords));
    }

    private void awaitUntilMessageIsPersisted(DBObject query) throws Exception {
        await().atMost(ONE_SECOND).until(() -> {
            DBCursor cursor = database.getCollection(COLLECTION_PUBLISHED_NAME).find(query);
            return cursor.size() == 1;
        });
    }

    private DBObject constructExpectedLogQuery(String topic,
                                               String id,
                                               String hostname,
                                               String reason,
                                               PublishedMessageTraceStatus status,
                                               String... extraRequestHeadersKeywords) {
        DBObject reasonCondition = start(REASON).is(reason).get();
        DBObject remainingConditions = constructExpectedLogQuery(topic, id, hostname, status, extraRequestHeadersKeywords);
        return start()
            .and(remainingConditions, reasonCondition)
            .get();
    }

    private DBObject constructExpectedLogQuery(String topic,
                                               String id,
                                               String hostname,
                                               PublishedMessageTraceStatus status,
                                               String... extraRequestHeadersKeywords) {
        DBObject basicConditions =
            start(TOPIC_NAME).is(topic)
                .put(STATUS).is(status.toString())
                .put(MESSAGE_ID).is(id)
                .put(SOURCE_HOSTNAME).is(hostname)
                .get();

        return start()
            .and(basicConditions)
            .and(constructExtraRequestHeadersConditions(extraRequestHeadersKeywords))
            .get();
    }

    private DBObject[] constructExtraRequestHeadersConditions(String... extraRequestHeadersKeywords) {
        DBObject[] conditions = new DBObject[extraRequestHeadersKeywords.length];
        for (int i = 0; i < extraRequestHeadersKeywords.length; ++i) {
            conditions[i] = start(EXTRA_REQUEST_HEADERS)
                .regex(compile(quote(extraRequestHeadersKeywords[i])))
                .get();
        }
        return conditions;
    }
}
