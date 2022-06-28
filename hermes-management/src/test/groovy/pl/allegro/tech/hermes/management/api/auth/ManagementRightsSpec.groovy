package pl.allegro.tech.hermes.management.api.auth

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.management.config.GroupProperties
import pl.allegro.tech.hermes.test.helper.builder.GroupBuilder
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Cookie
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Request
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext
import javax.ws.rs.core.UriInfo
import java.security.Principal

class ManagementRightsSpec extends Specification {

    @Unroll
    def "should check nonAdminCreationEnabled = #nonAdminCreationEnabled on group creation for admin = #admin"() {
        given:
        Group group = GroupBuilder.group("testGroup").build()

        def rights = createRights(nonAdminCreationEnabled)
        def groupRights = rights.getGroupCreatorRights(stubRequestContext(admin))

        when:
        def result = groupRights.allowedToCreate(group)

        then:
        result == expectedResult

        where:
        nonAdminCreationEnabled | admin || expectedResult
        true                    | true  || true
        false                   | true  || true
        true                    | false || true
        false                   | false || false
    }

    ContainerRequestContext stubRequestContext(boolean stubAdminRole) {
        return new ContainerRequestContext() {

            @Override
            Collection<String> getPropertyNames() {
                return null
            }

            @Override
            void removeProperty(String name) {

            }

            @Override
            UriInfo getUriInfo() {
                return null
            }

            @Override
            void setRequestUri(URI requestUri) {

            }

            @Override
            void setRequestUri(URI baseUri, URI requestUri) {

            }

            @Override
            Request getRequest() {
                return null
            }

            @Override
            String getMethod() {
                return null
            }

            @Override
            void setMethod(String method) {

            }

            @Override
            MultivaluedMap<String, String> getHeaders() {
                return null
            }

            @Override
            String getHeaderString(String name) {
                return null
            }

            @Override
            Date getDate() {
                return null
            }

            @Override
            Locale getLanguage() {
                return null
            }

            @Override
            int getLength() {
                return 0
            }

            @Override
            MediaType getMediaType() {
                return null
            }

            @Override
            List<MediaType> getAcceptableMediaTypes() {
                return null
            }

            @Override
            List<Locale> getAcceptableLanguages() {
                return null
            }

            @Override
            Map<String, Cookie> getCookies() {
                return null
            }

            @Override
            boolean hasEntity() {
                return false
            }

            @Override
            InputStream getEntityStream() {
                return null
            }

            @Override
            void setEntityStream(InputStream input) {

            }

            @Override
            SecurityContext getSecurityContext() {
                return new SecurityContext() {

                    @Override
                    Principal getUserPrincipal() {
                        return null
                    }

                    @Override
                    boolean isUserInRole(String role) {
                        return role == 'admin' && stubAdminRole
                    }

                    @Override
                    boolean isSecure() {
                        return false
                    }

                    @Override
                    String getAuthenticationScheme() {
                        return null
                    }
                }
            }

            @Override
            void setSecurityContext(SecurityContext context) {

            }

            @Override
            void abortWith(Response response) {

            }
        }
    }

    private ManagementRights createRights(boolean nonAdminCreationEnabled) {
        new ManagementRights(createGroupProperties(nonAdminCreationEnabled))
    }

    GroupProperties createGroupProperties(boolean nonAdminCreationEnabled) {
        def properties = new GroupProperties()
        properties.setNonAdminCreationEnabled(nonAdminCreationEnabled)
        return properties
    }
}
