package pl.allegro.tech.hermes.frontend;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;

@Configuration
public class SslContextFactoryConfiguration {

    @Bean
    @Profile("shouldInjectCustomSslContextFactoryToFrontend")
    public SslContextFactory mockSslContextFactory() {
        return Mockito.mock(SslContextFactory.class);
    }
}
