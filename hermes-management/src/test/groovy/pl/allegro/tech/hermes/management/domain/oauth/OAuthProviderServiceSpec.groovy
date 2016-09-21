package pl.allegro.tech.hermes.management.domain.oauth

import com.fasterxml.jackson.databind.ObjectMapper
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.clazz.EntityDefinitionBuilder
import org.junit.Before
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.allegro.tech.hermes.api.OAuthProvider
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions
import pl.allegro.tech.hermes.management.infrastructure.audit.AnonymizingAuditor
import pl.allegro.tech.hermes.management.infrastructure.audit.LoggingAuditor
import pl.allegro.tech.hermes.management.utils.MockAppender
import pl.allegro.tech.hermes.test.helper.builder.OAuthProviderBuilder
import spock.lang.Specification

import static org.slf4j.Logger.ROOT_LOGGER_NAME

class OAuthProviderServiceSpec extends Specification {

    static final String TEST_USER = "testUser"
    static final String CLIENT_SECRET = "CLIENT_SECRET_VALUE"
    static final String UPDATED_CLIENT_SECRET = "UPDATED_CLIENT_SECRET_VALUE"

    MockAppender mockAppender
    AnonymizingAuditor auditor = new AnonymizingAuditor(new LoggingAuditor(javers(), new ObjectMapper()))

    OAuthProviderRepository repository = Stub()

    @Before
    def createAndAddMockAppenderToLogger() {
        Logger root = LoggerFactory.getLogger(ROOT_LOGGER_NAME)
        mockAppender = new MockAppender()
        root.addAppender(mockAppender)
    }

    def "should anonymize data when auditing operation"(){
        given:
            OAuthProviderService oAuthProviderService = new OAuthProviderService(repository, new ApiPreconditions(),
                    auditor)
            OAuthProvider toBeCreated = OAuthProviderBuilder.oAuthProvider("name")
                    .withClientSecret(CLIENT_SECRET)
                    .build()
        when:
            oAuthProviderService.createOAuthProvider(toBeCreated, TEST_USER)

        then:
            with(mockAppender.list.last().toString()) {
                it.contains(TEST_USER)
                it.contains(OAuthProvider.class.getSimpleName())
                it.contains(toBeCreated.name)
                !it.contains(CLIENT_SECRET)
            }
    }


    def Javers javers() {
        return JaversBuilder.javers()
                .registerEntity(EntityDefinitionBuilder.entityDefinition(OAuthProvider.class)
                    .withIdPropertyName("name")
                    .build())
                .build()
    }
}
