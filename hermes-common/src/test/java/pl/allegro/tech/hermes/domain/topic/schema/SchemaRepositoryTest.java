package pl.allegro.tech.hermes.domain.topic.schema;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Topic;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static java.util.Optional.of;
import static org.assertj.core.api.StrictAssertions.assertThat;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.api.SchemaSource.valueOf;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

@RunWith(MockitoJUnitRunner.class)
public class SchemaRepositoryTest {

    private SchemaCompiler<String> uppercaseCompiler = x -> x.value().toUpperCase();

    @Mock
    private CachedSchemaSourceProvider cachedSchemaSourceProvider;

    @Test
    public void shouldThrowExceptionWhenFailedToLoadSchema() {
        // given
        Topic topic = topic().build();
        when(cachedSchemaSourceProvider.get(topic)).thenThrow(new RuntimeException("Cannot load schema"));
        SchemaRepository<String> schemaRepository = schemaRepository(uppercaseCompiler);

        // when
        catchException(schemaRepository).getSchema(topic);

        // then
        assertThat((Exception) caughtException()).isInstanceOf(CouldNotLoadSchemaException.class);
    }

    @Test
    public void shouldThrowExceptionWhenFailedToCompileSchema() {
        // given
        Topic topic = topic().build();
        when(cachedSchemaSourceProvider.get(topic)).thenReturn(of(valueOf("abc")));
        SchemaRepository<String> schemaRepository = schemaRepository(schemaSource -> {throw new RuntimeException("Cannot compile");});

        // when
        catchException(schemaRepository).getSchema(topic);

        // then
        assertThat((Exception) caughtException()).isInstanceOf(CouldNotLoadSchemaException.class);
    }

    @Test
    public void shouldReturnCompiledSchema() {
        // given
        Topic topic = topic().build();
        when(cachedSchemaSourceProvider.get(topic)).thenReturn(of(valueOf("abc")));
        SchemaRepository<String> schemaRepository = schemaRepository(uppercaseCompiler);

        // when
        String schema = schemaRepository.getSchema(topic);

        // then
        assertThat(schema).isEqualTo("ABC");
    }

    private SchemaRepository<String> schemaRepository(SchemaCompiler<String> schemaCompiler) {
        return new SchemaRepository<>(Topic.ContentType.AVRO, cachedSchemaSourceProvider, schemaCompiler);
    }
}
