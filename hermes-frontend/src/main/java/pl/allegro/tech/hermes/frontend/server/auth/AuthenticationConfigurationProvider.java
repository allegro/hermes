package pl.allegro.tech.hermes.frontend.server.auth;

import javax.inject.Inject;
import java.util.Optional;

public class AuthenticationConfigurationProvider {

    @Inject
    @org.jvnet.hk2.annotations.Optional
    private AuthenticationConfiguration authenticationConfiguration;

    public Optional<AuthenticationConfiguration> getAuthenticationConfiguration() {
        return Optional.ofNullable(authenticationConfiguration);
    }
}
