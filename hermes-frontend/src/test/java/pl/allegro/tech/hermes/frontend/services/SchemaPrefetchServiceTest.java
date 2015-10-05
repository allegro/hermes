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
        when(avroSchemaRepo.canService(Topic.ContentType.AVRO)).thenReturn(true);
        when(jsonSchemaRepo.canService(Topic.ContentType.JSON)).thenReturn(true);
        schemaPrefetchService = new SchemaPrefetchService(ImmutableList.of(avroSchemaRepo, jsonSchemaRepo));
    }

    @Test
    public void shouldNotPrefetchForTopicsWithoutValidation() throws Exception {
        //given
        Topic topic = topic().withValidation(false).build();

        //when
        schemaPrefetchService.prefetchFor(topic);

        //then
        verify(avroSchemaRepo, never()).getSchema(any());
        verify(jsonSchemaRepo, never()).getSchema(any());
    }

    @Test
    public void shouldPrefetchForAvroTopics() throws Exception {
        //given
        Topic topic = topic().withValidation(true).withContentType(Topic.ContentType.AVRO).build();

        //when
        schemaPrefetchService.prefetchFor(topic);

        //then
        verify(avroSchemaRepo, times(1)).getSchema(topic);
        verify(jsonSchemaRepo, never()).getSchema(any());
    }

    @Test
    public void shouldPrefetchForJsonTopics() throws Exception {
        //given
        Topic topic = topic().withValidation(true).withContentType(Topic.ContentType.JSON).build();

        //when
        schemaPrefetchService.prefetchFor(topic);

        //then
        verify(jsonSchemaRepo, times(1)).getSchema(topic);
        verify(avroSchemaRepo, never()).getSchema(any());
    }
}