package pl.allegro.tech.hermes.domain.topic.schema;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static java.util.Optional.of;
import static org.assertj.core.api.StrictAssertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    public void shouldNotifyConsumerAboutReloadedSchema() throws InterruptedException {
        // given
        ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        SchemaRepository<String> schemaRepository = schemaRepository(uppercaseCompiler);
        verify(cachedSchemaSourceProvider).onReload(consumerCaptor.capture());
        CountDownLatch countDownLatch = new CountDownLatch(1);
        schemaRepository.onReload(stringTopicWithSchema -> {
            assertThat(stringTopicWithSchema.getTopic().getQualifiedName()).isEqualTo("group.topic");
            assertThat(stringTopicWithSchema.getSchema()).isEqualTo("ABC");
            countDownLatch.countDown();
        });

        // when
        ((Consumer<TopicWithSchema<SchemaSource>>) consumerCaptor.getValue()).accept(new TopicWithSchema<>(
                topic().withName("group.topic").build(),
                valueOf("abc")));

        // then
        assertThat(countDownLatch.await(300, TimeUnit.MILLISECONDS)).isTrue();
    }

    @Test
    public void shouldPrecompileSchemaForReloadedSource() {
        // given
        Topic topic = topic().withName("group.topic").build();
        ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        SchemaRepository<String> schemaRepository = schemaRepository(uppercaseCompiler);
        verify(cachedSchemaSourceProvider).onReload(consumerCaptor.capture());
        ((Consumer<TopicWithSchema<SchemaSource>>) consumerCaptor.getValue()).accept(new TopicWithSchema<>(topic, valueOf("abc")));

        // when
        String compiledSchema = schemaRepository.getSchema(topic);


        // then
        assertThat(compiledSchema).isEqualTo("ABC");
        verify(cachedSchemaSourceProvider, never()).get(topic);
    }

    private SchemaRepository<String> schemaRepository(SchemaCompiler<String> schemaCompiler) {
        return new SchemaRepository<>(ContentType.AVRO, cachedSchemaSourceProvider, schemaCompiler);
    }
}
