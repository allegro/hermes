# Publishing security

Hermes has a feature to restrict publishing on specific topics to particular services: [publishing permissions](/user/permissions).
How service name is extracted from request is entirely configurable and depends on security requirements within company's ecosystem.

Since Hermes uses Undertow as http server authentication implementation follows Undertow
internal security model which is described here: [Undertow Security](http://undertow.io/undertow-docs/undertow-docs-1.3.0/#security).

Authentication has to be enabled using following configuration:

Option                          | Description                                         | Options                       | Default value
------------------------------- | --------------------------------------------------- | ----------------------------- | -------------
frontend.authentication.enabled | enable authentication handler                       | true, false                   | false
frontend.authentication.mode    | in which circumstances perform authentication       | constraint_driven, pro_active | constraint_driven

More about authentication mode can be read here: [AuthenticationMode](http://undertow.io/javadoc/1.3.x/io/undertow/security/api/AuthenticationMode.html)

## Implementing IdentityManager

Authentication is handled by `IdentityManager` exposed via `AuthenticationConfiguration` from `HermesFrontend.Builder`.
Hermes by default do not come with any concrete implementation so it has to be coded.

Frontend is extracting service name from `HttpServerExchange` using following method:
```java
String serviceName = exchange.getSecurityContext().getAuthenticatedAccount().getPrincipal().getName();
```

Worth mentioning is that authenticated account has to contain role `Roles.PUBLISH` otherwise frontend will return 403
without evaluating topic specific authorisation configuration. This behavior can be used for instance to reject requests from
development environment on production.

Below you can see naive implementation of authentication via `Authorization` header used in our integration tests. 

```java 
Predicate<HttpServerExchange> isAuthenticationRequiredPredicate = exchange -> true;
AuthenticationMechanism basicAuth = new BasicAuthenticationMechanism("basicAuthRealm");
builder.withAuthenticationConfiguration(new AuthenticationConfiguration(
                                            isAuthenticationRequiredPredicate,
                                            Arrays.asList(basicAuth),
                                            new SingleUserAwareIdentityManager("John", "12345")                                          
                                        ));
```

```java
public class SingleUserAwareIdentityManager implements IdentityManager {
    private final String username;
    private final String password;

    SingleUserAwareIdentityManager(String username, String password) {
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
            return Collections.singleton(Roles.PUBLISHER);
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

    static Map<String, String> getHeadersWithAuthentication(String username, String password) {
        String credentials = username + ":" + password;
        String token = "Basic " + Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", token);
        return headers;
    }
}
```