# Access security

Since Management module uses Jersey as REST API library, access security is based on
[JSR 250 annotations](https://jcp.org/en/jsr/detail?id=250). By default there are no security restrictions.

Ownership model is described in [Ownership and permissions](/user/permissions) section. There are many ways to restrict
access to sensitive endpoints, for example:

* simple tokens/passwords generated for each **group** or **subscription**
* passwords generated for each **Support Team**
* OAuth tokens validated against **Support Team**

Security rules need to be coded as an implementation of `pl.allegro.tech.hermes.management.api.auth.SecurityContextProvider`
interface, which needs to be present in Management module Spring context (see
[packaging section](/deployment/packaging#management) for more information on Management customization).

```java
@Component
public class MyCustomSecurityContextProvider implements SecurityContextProvider {

    @Override
    public SecurityContext securityContext(ContainerRequestContext requestContext) {
        Strign username = extractUserFromRequest(requestContext);

        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                /* ... */
            }

            @Override
            public boolean isUserInRole(String role) {
                return myAuthorizationRepository.isUserInRole(username, role);
            }

            @Override
            public boolean isSecure() {
                /* ... */
            }

            @Override
            public String getAuthenticationScheme() {
                /* ... */
            }
        };
    }
}
```
