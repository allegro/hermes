package pl.allegro.tech.hermes.frontend;

import com.google.common.collect.Lists;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pl.allegro.tech.hermes.frontend.server.auth.AuthenticationConfiguration;

@Configuration
public class AuthConfiguration {

  @Value("${auth.username}")
  private String username;

  @Value("${auth.password}")
  private String password;

  @Bean
  @Profile("authRequired")
  public AuthenticationConfiguration requiredAuthenticationConfiguration() {
    return getAuthConfig(true);
  }

  @Bean
  @Profile("authNonRequired")
  public AuthenticationConfiguration notRequiredAuthenticationConfiguration() {
    return getAuthConfig(false);
  }

  private AuthenticationConfiguration getAuthConfig(boolean isAuthenticationRequired) {
    return new AuthenticationConfiguration(
        exchange -> isAuthenticationRequired,
        Lists.newArrayList(new BasicAuthenticationMechanism("basicAuthRealm")),
        new SingleUserAwareIdentityManager(username, password));
  }
}
