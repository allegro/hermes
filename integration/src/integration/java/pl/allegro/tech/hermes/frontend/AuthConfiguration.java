package pl.allegro.tech.hermes.frontend;

import com.google.common.collect.Lists;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import pl.allegro.tech.hermes.frontend.server.auth.AuthenticationConfiguration;
import pl.allegro.tech.hermes.integration.auth.SingleUserAwareIdentityManager;

@Configuration
@PropertySource("classpath:application-auth.properties")
public class AuthConfiguration {

    @Value("${auth.username}")
    private String username;

    @Value("${auth.password}")
    private String password;

    @Bean
    @Primary
    @Profile("authRequired")
    public AuthenticationConfiguration authenticationConfiguration() {
        return getAuthConfig(true);
    }

    @Bean
    @Primary
    @Profile("authNonRequired")
    public AuthenticationConfiguration authenticationConfiguration2() {
        return getAuthConfig(false);
    }

    private AuthenticationConfiguration getAuthConfig (boolean isAuthenticationRequired) {
        return new AuthenticationConfiguration(
                exchange -> isAuthenticationRequired,
                Lists.newArrayList(new BasicAuthenticationMechanism("basicAuthRealm")),
                new SingleUserAwareIdentityManager(username, password));
    }

}
