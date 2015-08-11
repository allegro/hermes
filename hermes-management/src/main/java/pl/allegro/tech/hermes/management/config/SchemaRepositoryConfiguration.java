package pl.allegro.tech.hermes.management.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.avro.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.TopicFieldSchemaSourceProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(SchemaCacheProperties.class)
public class SchemaRepositoryConfiguration {

    @Autowired
    SchemaCacheProperties schemaCacheProperties;

    @Bean
    public SchemaSourceProvider schemaSourceProvider() {
        return new TopicFieldSchemaSourceProvider();
    }

    @Bean
    public SchemaRepository<Schema> avroSchemaRepository(SchemaSourceProvider schemaSourceProvider) {
        return new SchemaRepository<>(
                schemaSourceProvider,
                createSchemaReloadExecutorService(schemaCacheProperties.getPoolSize(), "avro"),
                schemaCacheProperties.getRefreshAfterWriteMinutes(),
                schemaCacheProperties.getExpireAfterWriteMinutes(),
                source -> new Schema.Parser().parse(source.value()));
    }

    private ExecutorService createSchemaReloadExecutorService(int poolSize, String format) {
        return Executors.newFixedThreadPool(poolSize, new ThreadFactoryBuilder().setNameFormat(format + "-schema-reloader-%d").build());
    }
}
