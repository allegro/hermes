package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.base.Ticker;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executors;

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
        return new SchemaRepository<>(new ConfigFactory(), sourceRepository, MoreExecutors.sameThreadExecutor(), uppercaseCompiler);
    }

    private SchemaRepository<String> schemaRepository(SchemaSourceProvider sourceRepository, Ticker ticker) {
        return new SchemaRepository<>(new ConfigFactory(), sourceRepository, MoreExecutors.sameThreadExecutor(), ticker, uppercaseCompiler);
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
