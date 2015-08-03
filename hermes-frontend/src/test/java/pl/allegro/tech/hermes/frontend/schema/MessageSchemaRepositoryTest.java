package pl.allegro.tech.hermes.frontend.schema;

import com.google.common.base.Ticker;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Topic;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.StrictAssertions.assertThat;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

public class MessageSchemaRepositoryTest {

    private MessageSchemaCompiler<String> uppercaseCompiler = String::toUpperCase;

    @Test
    public void shouldThrowExceptionWhenFailedToLoadSchema() {
        // given
        MessageSchemaRepository<String> schemaRepository = messageSchemaRepository(topic -> {
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
        MessageSchemaRepository<String> schemaRepository = messageSchemaRepository(topic -> null);
        Topic topic = topic().build();

        // when
        catchException(schemaRepository).getSchema(topic);

        // then
        assertThat((Exception) caughtException()).isInstanceOf(CouldNotLoadSchemaException.class);
    }

    @Test
    public void shouldReturnCompiledSchema() {
        // given
        MessageSchemaRepository<String> schemaRepository = messageSchemaRepository(Topic::getMessageSchema);
        Topic topic = topic().withMessageSchema("abc").build();

        // when
        String schema = schemaRepository.getSchema(topic);

        // then
        assertThat(schema).isEqualTo("ABC");
    }

    @Test
    public void shouldCacheSchema() {
        // given
        Queue<String> sources = new LinkedList(Arrays.asList("s1", "s2"));
        FakeTicker ticker = new FakeTicker();
        MessageSchemaRepository<String> schemaRepository = messageSchemaRepository(topic -> sources.poll(), ticker);
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
        Queue<String> sources = new LinkedList(Arrays.asList("s1", "s2"));
        FakeTicker ticker = new FakeTicker();
        MessageSchemaRepository<String> schemaRepository = messageSchemaRepository(topic -> sources.poll(), ticker);
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
        Queue<String> sources = new LinkedList(Arrays.asList("s1"));
        FakeTicker ticker = new FakeTicker();
        MessageSchemaRepository<String> schemaRepository = messageSchemaRepository(topic -> sources.remove(), ticker);
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
        Queue<String> sources = new LinkedList(Arrays.asList("s1", null));
        FakeTicker ticker = new FakeTicker();
        MessageSchemaRepository<String> schemaRepository = messageSchemaRepository(topic -> sources.poll(), ticker);
        Topic topic = topic().build();

        // when
        schemaRepository.getSchema(topic);
        ticker.advance(Duration.ofMinutes(11));
        String schema2 = schemaRepository.getSchema(topic);

        // then
        assertThat(schema2).isEqualTo("S1");
    }

    private MessageSchemaRepository<String> messageSchemaRepository(MessageSchemaSourceRepository sourceRepository) {
        return new MessageSchemaRepository<>(sourceRepository, Executors.newSingleThreadExecutor(), uppercaseCompiler);
    }

    private MessageSchemaRepository<String> messageSchemaRepository(MessageSchemaSourceRepository sourceRepository, Ticker ticker) {
        return new MessageSchemaRepository<>(sourceRepository, Executors.newSingleThreadExecutor(), ticker, uppercaseCompiler);
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
