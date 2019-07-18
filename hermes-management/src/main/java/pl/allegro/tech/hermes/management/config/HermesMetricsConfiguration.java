package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pl.allegro.tech.hermes.common.di.factories.MetricRegistryFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.util.InetAddressHostnameResolver;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

@Import({
        pl.allegro.tech.hermes.common.di.factories.MetricRegistryFactory.class,
        pl.allegro.tech.hermes.common.config.ConfigFactory.class,
        pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterStorage.class,
        pl.allegro.tech.hermes.common.util.InetAddressHostnameResolver.class
})
@Configuration
public class HermesMetricsConfiguration {

    @Bean
    HermesMetrics hermesMetrics(MetricRegistryFactory metricRegistryFactory, PathsCompiler pathsCompiler) {
        return new HermesMetrics(
                metricRegistryFactory.provide(),
                pathsCompiler
        );
    }

    @Bean
    PathsCompiler pathsCompiler(InetAddressHostnameResolver inetAddressHostnameResolver) {
        return new PathsCompiler(inetAddressHostnameResolver.resolve());
    }

    @Bean
    String moduleName() {
        return "management";
    }
}
