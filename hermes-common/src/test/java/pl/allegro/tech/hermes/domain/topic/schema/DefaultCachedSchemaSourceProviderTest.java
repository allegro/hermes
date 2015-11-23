package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.base.Ticker;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.StrictAssertions.assertThat;
import static pl.allegro.tech.hermes.api.SchemaSource.valueOf;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

public class DefaultCachedSchemaSourceProviderTest {

    private FakeTicker ticker;

    @Before
    public void init() {
        ticker = new FakeTicker();
    }

    @Test
    public void shouldCacheSchema() {
        // given
        Queue<SchemaSource> sources = new LinkedList<>(asList(valueOf("s1"), valueOf("s2")));
        CachedSchemaSourceProvider cachedSchema = cachedSchemaSourceProvider(topic -> ofNullable(sources.poll()));
        Topic topic = topic().build();

        // when
        Optional<SchemaSource> schema1 = cachedSchema.get(topic);
        ticker.advance(ofMinutes(5));
        Optional<SchemaSource> schema2 = cachedSchema.get(topic);

        // then
        assertThat(schema2.get()).isEqualTo(schema1.get()).isEqualTo(valueOf("s1"));
    }

    @Test
    public void shouldReloadSchemaAfterExpiration() {
        // given
        Queue<SchemaSource> sources = new LinkedList<>(asList(valueOf("s1"), valueOf("s2")));
        CachedSchemaSourceProvider cachedSchema = cachedSchemaSourceProvider(topic -> ofNullable(sources.poll()));
        Topic topic = topic().build();

        // when
        cachedSchema.get(topic);
        ticker.advance(ofMinutes(11));
        Optional<SchemaSource> schema2 = cachedSchema.get(topic);

        // then
        assertThat(schema2.get()).isEqualTo(valueOf("s2"));
    }

    @Test
    public void shouldNotifyConsumersAboutSchemaReload() throws InterruptedException {
        // given
        CountDownLatch reloadSchemaLatch = new CountDownLatch(1);
        Queue<SchemaSource> sources = new LinkedList<>(asList(valueOf("s1"), valueOf("s2")));
        CachedSchemaSourceProvider cachedSchema = cachedSchemaSourceProvider(topic -> ofNullable(sources.poll()));
        cachedSchema.onReload(topicWithSchema -> {
            assertThat(topicWithSchema.getSchema()).isEqualTo(valueOf("s2"));
            reloadSchemaLatch.countDown();
        });
        Topic topic = topic().build();

        // when
        cachedSchema.get(topic);
        ticker.advance(ofMinutes(11));
        cachedSchema.get(topic);

        // then
        assertThat(reloadSchemaLatch.await(300, MILLISECONDS)).isTrue();
    }

    @Test
    public void shouldNotifyConsumersAboutSchemaRemove() throws InterruptedException {
        // given
        Topic someTopic = topic().withName("old.topic").build();
        CountDownLatch removeSchemaLatch = new CountDownLatch(1);
        CachedSchemaSourceProvider cachedSchema = cachedSchemaSourceProvider(topic -> ofNullable(valueOf("schema")));
        cachedSchema.onRemove(topicWithSchema -> {
            assertThat(topicWithSchema.getTopic().getQualifiedName()).isEqualTo(someTopic.getQualifiedName());
            removeSchemaLatch.countDown();
        });

        // when
        cachedSchema.get(someTopic);
        ticker.advance(ofMinutes(60 * 24 + 1));
        cachedSchema.get(someTopic);

        //then
        assertThat(removeSchemaLatch.await(300, MILLISECONDS)).isTrue();
    }

    @Test
    public void shouldReturnOldSchemaWhenSchemaReloadingFailed() {
        // given
        Queue<SchemaSource> sources = new LinkedList<>(asList(valueOf("s1")));
        CachedSchemaSourceProvider cachedSchema = cachedSchemaSourceProvider(topic -> ofNullable(sources.remove()));
        Topic topic = topic().build();

        // when
        cachedSchema.get(topic);
        ticker.advance(ofMinutes(11));
        Optional<SchemaSource> schema2 = cachedSchema.get(topic);

        // then
        assertThat(schema2.get()).isEqualTo(valueOf("s1"));
    }

    private CachedSchemaSourceProvider cachedSchemaSourceProvider(SchemaSourceProvider schemaSourceProvider) {
        return new DefaultCachedSchemaSourceProvider(
                10, 60 * 24, MoreExecutors.sameThreadExecutor(), schemaSourceProvider, ticker);
    }

    private static class FakeTicker extends Ticker {

        private long currentNanos = 0;

        @Override
        public long read() {
            return currentNanos;
        }

        public void advance(Duration duration) {
            currentNanos += duration.toNanos();
        }
    }

}