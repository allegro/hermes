package pl.allegro.tech.hermes.integration;

import avro.shaded.com.google.common.collect.Lists;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.util.StatusCodes;
import org.apache.commons.codec.binary.Base64;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.frontend.server.AuthenticationConfiguration;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class FrontendAuthenticationConfigurationTest extends IntegrationTest {

    public static final int FRONTEND_PORT = Ports.nextAvailable();
    public static final String FRONTEND_URL = "http://127.0.0.1:" + FRONTEND_PORT;

    private static final String USERNAME = "someUser";
    private static final String PASSWORD = "somePassword123";

    protected HermesPublisher publisher;
    protected HermesServer hermesServer;

    private HermesFrontend hermesFrontend;

    @BeforeClass
    public void setup() throws Exception {
        ConfigFactory configFactory = new MutableConfigFactory()
                .overrideProperty(Configs.FRONTEND_PORT, FRONTEND_PORT)
                .overrideProperty(Configs.FRONTEND_SSL_ENABLED, false)
                .overrideProperty(Configs.FRONTEND_AUTHENTICATION_ENABLED, true);

        AuthenticationConfiguration authConfig = new AuthenticationConfiguration(
                exchange -> true,
                Lists.newArrayList(new BasicAuthenticationMechanism("basicAuthRealm")),
                new SingleUserAwareIdentityManager(USERNAME, PASSWORD));

        hermesFrontend = HermesFrontend.frontend()
                .withBinding(configFactory, ConfigFactory.class)
                .withBinding(authConfig, AuthenticationConfiguration.class)
                .build();

        hermesFrontend.start();

        hermesServer = hermesFrontend.getService(HermesServer.class);
        publisher = new HermesPublisher(FRONTEND_URL);

        operations.buildTopic("someGroup", "topicWithAuthorization");
    }

    @AfterClass
    public void tearDown() throws InterruptedException {
        hermesFrontend.stop();
    }

    @Test
    public void shouldAuthenticateUsingBasicAuth() throws Throwable {
        //given
        Map<String, String> headers = getHeadersWithAuthentication(USERNAME, PASSWORD);

        //when
        Response response = publisher.publish("someGroup.topicWithAuthorization",
                TestMessage.of("hello", "world").body(), headers);

        //then
        assertThat(response.getStatusInfo().getFamily()).isEqualTo(SUCCESSFUL);
    }

    @Test
    public void shouldNotAuthenticateUserWithInvalidCredentials() throws Throwable {
        //given
        Map<String, String> headers = getHeadersWithAuthentication(USERNAME, "someInvalidPassword");

        //when
        Response response = publisher.publish("someGroup.topicWithAuthorization",
                TestMessage.of("hello", "world").body(), headers);

        //then
        assertThat(response.getStatus()).isEqualTo(StatusCodes.UNAUTHORIZED);
    }

    @Test
    public void shouldNotAuthenticateUserWithoutCredentials() throws Throwable {
        //when
        Response response = publisher.publish("someGroup.topicWithAuthorization",
                TestMessage.of("hello", "world").body());

        //then
        assertThat(response.getStatus()).isEqualTo(StatusCodes.UNAUTHORIZED);
    }

    private Map<String, String> getHeadersWithAuthentication(String username, String password) {
        String credentials = username + ":" + password;
        String token = "Basic " + Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", token);
        return headers;
    }

    private final class SingleUserAwareIdentityManager implements IdentityManager {

        private final String username;
        private final String password;

        private SingleUserAwareIdentityManager(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public Account verify(Account account) {
            return null;
        }

        @Override
        public Account verify(String username, Credential credential) {
            String password = new String(((PasswordCredential) credential).getPassword());
            if (this.username.equals(username) && this.password.equals(password)) {
                return new SomeUserAccount(username);
            }
            return null;
        }

        @Override
        public Account verify(Credential credential) {
            return null;
        }
    }

    private final class SomeUserAccount implements Account {

        private final Principal principal;

        private SomeUserAccount(String username) {
            this.principal = new SomeUserPrincipal(username);
        }

        @Override
        public Principal getPrincipal() {
            return principal;
        }

        @Override
        public Set<String> getRoles() {
            return Collections.emptySet();
        }
    }

    private final class SomeUserPrincipal implements Principal {

        private final String username;

        private SomeUserPrincipal(String username) {
            this.username = username;
        }

        @Override
        public String getName() {
            return username;
        }
    }
}
