package pl.allegro.tech.hermes.frontend.services;

import com.github.fge.jsonschema.main.JsonSchema;
import com.google.common.collect.ImmutableList;
import org.apache.avro.Schema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import static org.mockito.Mockito.*;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

@RunWith(MockitoJUnitRunner.class)
public class SchemaPrefetchServiceTest {
    @Mock
    private SchemaRepository<Schema> avroSchemaRepo;

    @Mock
    private SchemaRepository<JsonSchema> jsonSchemaRepo;

    private SchemaPrefetchService schemaPrefetchService;

    @Before
    public void before() {
        when(avroSchemaRepo.supportedContentType()).thenReturn(Topic.ContentType.AVRO);
        when(jsonSchemaRepo.supportedContentType()).thenReturn(Topic.ContentType.JSON);
        schemaPrefetchService = new SchemaPrefetchService(ImmutableList.of(avroSchemaRepo, jsonSchemaRepo));
    }

    @Test
    public void shouldPrefetchSchemaFromEveryAvailableSchemaRepo() throws Exception {
        //given
        Topic topic = topic().build();

        //when
        schemaPrefetchService.prefetchFor(topic);

        //then
        verify(avroSchemaRepo).getSchema(any());
        verify(jsonSchemaRepo).getSchema(any());
    }

    @Test
    public void shouldHandleExceptionWhileFetchingSchema() {
        //given
        Topic topic = topic().build();
        when(avroSchemaRepo.getSchema(any())).thenThrow(new RuntimeException());

        //when
        schemaPrefetchService.prefetchFor(topic);

        //then
        verify(avroSchemaRepo).getSchema(topic);
        verify(jsonSchemaRepo).getSchema(any());
    }

}