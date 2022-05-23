package pl.allegro.tech.hermes.common.config;

import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.di.factories.CuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.HermesCuratorClientFactory;

@Configuration
public class ConfigCommonTest {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public CuratorFramework hermesCurator(ConfigFactory configFactory, CuratorClientFactory curatorClientFactory) {
        return new HermesCuratorClientFactory(configFactory, curatorClientFactory).provide();
    }
}
