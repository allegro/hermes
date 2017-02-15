# Access security

Since Management module uses Jersey as REST API library, access security is based on
[JSR 250 annotations](https://jcp.org/en/jsr/detail?id=250). By default there are no security restrictions.

Ownership model is described in [Ownership and permissions](/user/permissions) section. There are many ways to restrict
access to sensitive endpoints, for example:

* simple tokens/passwords generated for each **group** or **subscription**
* passwords generated for each **Owner**
* OAuth tokens validated against **Owner**

Security rules need to be coded as an implementation of `pl.allegro.tech.hermes.management.api.auth.SecurityProvider`
interface, which needs to be present in Management module Spring context (see
[packaging section](/deployment/packaging#management) for more information on Management customization).

```java
@Component
public class MyCustomSecurityProvider implements SecurityProvider {

    @Override
    public HermesSecurity security(ContainerRequestContext requestContext) {
        return new HermesSecurity(securityContext(requestContext), ownershipResolver(requestContext));
    }

    private SecurityContext securityContext(ContainerRequestContext requestContext) {
        Strign username = extractUserFromRequest(requestContext);

        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return new Principal() {
                    @Override
                    public String getName() {
                        return username;
                    }
                };
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
    
    private OwnershipResolver ownershipResolver(ContainerRequestContext requestContext) {
        /* ... */
    }
}
```


## Management operations auditing

Hermes includes management operations auditor, which audits group, topic or subscription creation, removal or modification.
It can be configured using following options:


Option                    | Description                            | Default value
------------------------- | -------------------------------------- | -------------
enabled                   | enable Auditor                         | false


Auditor uses `java.security.Principal` obtained from `javax.ws.rs.core.SecurityContext` to get user name. 
`SecurityContext` is provided by component implementing `pl.allegro.tech.hermes.management.api.auth.SecurityContextProvider` as in `MyCustomSecurityContextProvider` example above.
Currently basic `pl.allegro.tech.hermes.management.infrastructure.audit.LoggingAuditor` is provided.