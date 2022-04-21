package pl.allegro.tech.hermes.frontend;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import pl.allegro.tech.hermes.benchmark.environment.InMemorySchemaClient;
import pl.allegro.tech.hermes.schema.RawSchemaClient;

import java.io.IOException;

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;
import static pl.allegro.tech.hermes.benchmark.environment.FrontendEnvironment.loadMessageResource;

@Configuration
@State(Scope.Benchmark)
public class BenchmarkConfiguration {

    @Bean
    @Primary
    @Profile("benchmark")
    RawSchemaClient inMemorySchemaClient() throws IOException {
        return new InMemorySchemaClient(fromQualifiedName("bench.topic"), loadMessageResource("schema"), 1, 1);
    }

}
