package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.base.Ticker;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.StrictAssertions.assertThat;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

public class SchemaRepositoryTest {

    private SchemaCompiler<String> uppercaseCompiler = x -> x.value().toUpperCase();

    @Test
    public void shouldThrowExceptionWhenFailedToLoadSchema() {
        // given
        SchemaRepository<String> schemaRepository = schemaRepository(topic -> {
            throw new RuntimeException("Cannot load schema");
        });
        Topic topic = topic().build();

        // when
        catchException(schemaRepository).getSchema(topic);

        // then
        assertThat((Exception) caughtException()).isInstanceOf(CouldNotLoadSchemaException.class);
    }

    @Test
    public void shouldThrowExceptionWhenFailedToCompileSchema() {
        // given
        SchemaRepository<String> schemaRepository = schemaRepository(topic -> null);
        Topic topic = topic().build();

        // when
        catchException(schemaRepository).getSchema(topic);

        // then
        assertThat((Exception) caughtException()).isInstanceOf(CouldNotLoadSchemaException.class);
    }

    @Test
    public void shouldReturnCompiledSchema() {
        // given
        SchemaRepository<String> schemaRepository = schemaRepository(new TopicFieldSchemaSourceProvider());
        Topic topic = topic().withMessageSchema("abc").build();

        // when
        String schema = schemaRepository.getSchema(topic);

        // then
        assertThat(schema).isEqualTo("ABC");
    }

    @Test
    public void shouldCacheSchema() {
        // given
        Queue<SchemaSource> sources = new LinkedList<>(Arrays.asList(SchemaSource.valueOf("s1"), SchemaSource.valueOf("s2")));
        FakeTicker ticker = new FakeTicker();
        SchemaRepository<String> schemaRepository = schemaRepository(topic -> Optional.ofNullable(sources.poll()), ticker);
        Topic topic = topic().build();

        // when
        String schema1 = schemaRepository.getSchema(topic);
        ticker.advance(Duration.ofMinutes(5));
        String schema2 = schemaRepository.getSchema(topic);

        // then
        assertThat(schema2).isEqualTo(schema1).isEqualTo("S1");
    }

    @Test
    public void shouldReloadSchemaAfterExpiration() {
        // given
        Queue<SchemaSource> sources = new LinkedList<>(Arrays.asList(SchemaSource.valueOf("s1"), SchemaSource.valueOf("s2")));
        FakeTicker ticker = new FakeTicker();
        SchemaRepository<String> schemaRepository = schemaRepository(topic -> Optional.ofNullable(sources.poll()), ticker);
        Topic topic = topic().build();

        // when
        schemaRepository.getSchema(topic);
        ticker.advance(Duration.ofMinutes(11));
        String schema2 = schemaRepository.getSchema(topic);

        // then
        assertThat(schema2).isEqualTo("S2");
    }

    @Test
    public void shouldNotifyConsumersAboutSchemaReload() throws InterruptedException {
        // given
        CountDownLatch reloadSchemaLatch = new CountDownLatch(1);
        Queue<SchemaSource> sources = new LinkedList<>(Arrays.asList(SchemaSource.valueOf("s1"), SchemaSource.valueOf("s2")));
        FakeTicker ticker = new FakeTicker();
        SchemaRepository<String> schemaRepository = schemaRepository(topic -> Optional.ofNullable(sources.poll()), ticker);
        schemaRepository.onReload(stringTopicWithSchema -> {
            assertThat(stringTopicWithSchema.getSchema()).isEqualTo("S2");
            reloadSchemaLatch.countDown();
        });
        Topic topic = topic().build();

        // when
        schemaRepository.getSchema(topic);
        ticker.advance(Duration.ofMinutes(11));
        schemaRepository.getSchema(topic);

        // then
        assertThat(reloadSchemaLatch.await(300, TimeUnit.MILLISECONDS)).isTrue();
    }

    @Test
    public void shouldNotifyConsumersAboutSchemaRemove() throws InterruptedException {
        // given
        CountDownLatch removeSchemaLatch = new CountDownLatch(1);
        FakeTicker ticker = new FakeTicker();
        SchemaRepository<String> schemaRepository = schemaRepository(topic -> Optional.ofNullable(SchemaSource.valueOf("schema")), ticker);
        schemaRepository.onRemove(stringTopicWithSchema -> {
            assertThat(stringTopicWithSchema.getTopic().getQualifiedName()).isEqualTo("old.topic");
            removeSchemaLatch.countDown();
        });

        // when
        schemaRepository.getSchema(topic().withName("old.topic").build());
        ticker.advance(Duration.ofMinutes(60 * 24 + 1));
        schemaRepository.getSchema(topic().withName("new.topic1").build());
        schemaRepository.getSchema(topic().withName("new.topic2").build());

        //then
        assertThat(removeSchemaLatch.await(300, TimeUnit.MILLISECONDS)).isTrue();
    }

    @Test
    public void shouldReturnOldSchemaWhenSchemaReloadingFailed() {
        // given
        Queue<SchemaSource> sources = new LinkedList<>(Arrays.asList(SchemaSource.valueOf("s1")));
        FakeTicker ticker = new FakeTicker();
        SchemaRepository<String> schemaRepository = schemaRepository(topic -> Optional.ofNullable(sources.remove()), ticker);
        Topic topic = topic().build();

        // when
        schemaRepository.getSchema(topic);
        ticker.advance(Duration.ofMinutes(11));
        String schema2 = schemaRepository.getSchema(topic);

        // then
        assertThat(schema2).isEqualTo("S1");
    }

    @Test
    public void shouldReturnOldSchemaWhenFailedToCompileReloadedSchema() {
        // given
        Queue<SchemaSource> sources = new LinkedList<>(Arrays.asList(SchemaSource.valueOf("s1"), null));
        FakeTicker ticker = new FakeTicker();
        SchemaRepository<String> schemaRepository = schemaRepository(topic -> Optional.ofNullable(sources.poll()), ticker);
        Topic topic = topic().build();

        // when
        schemaRepository.getSchema(topic);
        ticker.advance(Duration.ofMinutes(11));
        String schema2 = schemaRepository.getSchema(topic);

        // then
        assertThat(schema2).isEqualTo("S1");
    }

    private SchemaRepository<String> schemaRepository(SchemaSourceProvider sourceRepository) {
        return new SchemaRepository<>(sourceRepository, MoreExecutors.sameThreadExecutor(), 10, 60 * 24, uppercaseCompiler);
    }

    private SchemaRepository<String> schemaRepository(SchemaSourceProvider sourceRepository, Ticker ticker) {
        return new SchemaRepository<>(sourceRepository, MoreExecutors.sameThreadExecutor(), ticker, 10, 60 * 24, uppercaseCompiler);
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
