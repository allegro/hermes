package pl.allegro.tech.hermes.schema.confluent

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy
import com.github.tomakehurst.wiremock.client.WireMock
import pl.allegro.tech.hermes.api.RawSchema
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.schema.CouldNotRemoveSchemaException
import pl.allegro.tech.hermes.schema.InvalidSchemaException
import pl.allegro.tech.hermes.schema.RawSchemaClient
import pl.allegro.tech.hermes.schema.SchemaRepositoryServerException
import pl.allegro.tech.hermes.schema.SchemaVersion
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import javax.ws.rs.client.ClientBuilder

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class SchemaRegistryRawSchemaClientTest extends Specification {

    @Shared String schemaRegistryContentType = "application/vnd.schemaregistry.v1+json"

    @Shared TopicName topicName = TopicName.fromQualifiedName("someGroup.someTopic")

    @Shared RawSchema rawSchema = RawSchema.valueOf("{}")

    @Shared WireMockServer wireMock

    @Shared @Subject RawSchemaClient client

    def setupSpec() {
        def port = Ports.nextAvailable()
        wireMock = new WireMockServer(port)
        wireMock.start()
        client = new SchemaRegistryRawSchemaClient(ClientBuilder.newClient(), URI.create("http://localhost:$port"))
    }

    def cleanupSpec() {
        wireMock.stop()
    }

    def setup() {
        wireMock.resetMappings()
        wireMock.resetRequests()
    }

    def "should register schema"() {
        given:
        wireMock.stubFor(post(versionsUrl(topicName)).willReturn(okResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        wireMock.verify(1, postRequestedFor(versionsUrl(topicName))
                .withHeader("Content-type", equalTo(schemaRegistryContentType))
                .withRequestBody(equalTo("""{"schema":"{}"}""")))
    }

    def "should throw invalid schema exception for invalid schema registration"() {
        given:
        wireMock.stubFor(post(versionsUrl(topicName)).willReturn(badRequestResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        thrown(InvalidSchemaException)
    }

    def "should throw repository server exception for server internal error response when registering schema"() {
        given:
        wireMock.stubFor(post(versionsUrl(topicName)).willReturn(internalErrorResponse()))

        when:
        client.registerSchema(topicName, rawSchema)

        then:
        thrown(SchemaRepositoryServerException)
    }

    def "should fetch schema at specified version"() {
        given:
        def version = 5
        wireMock.stubFor(get(schemaVersionUrl(topicName, version)).willReturn(okResponse()
                .withHeader("Content-type", "application/json")
                .withBody("""{"subject":"someGroup.someTopic","id":100,"version":$version,"schema":"{}"}""")))

        when:
        def schema = client.getSchema(topicName, SchemaVersion.valueOf(version))

        then:
        schema.get() == rawSchema
    }

    def "should fetch latest schema version"() {
        given:
        wireMock.stubFor(get(schemaLatestVersionUrl(topicName)).willReturn(okResponse()
                .withHeader("Content-type", "application/json")
                .withBody("""{"subject":"someGroup.someTopic","id":200,"version":20,"schema":"{}"}""")))

        when:
        def schema = client.getLatestSchema(topicName)

        then:
        schema.get() == rawSchema
    }

    def "should return empty optional when latest schema does not exist"() {
        when:
        def schema = client.getLatestSchema(topicName)

        then:
        !schema.isPresent()
    }

    def "should return all schema versions"() {
        given:
        wireMock.stubFor(get(versionsUrl(topicName)).willReturn(okResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("[3,6,2]")))

        when:
        def versions = client.getVersions(topicName)

        then:
        versions.containsAll(SchemaVersion.valueOf(3), SchemaVersion.valueOf(6), SchemaVersion.valueOf(2))
    }

    def "should return empty list on not registered subject"() {
        when:
        def versions = client.getVersions(topicName)

        then:
        versions.isEmpty()
    }

    def "should delete all schema versions"() {
        given:
        wireMock.stubFor(WireMock.delete(versionsUrl(topicName)).willReturn(okResponse()))

        when:
        client.deleteAllSchemaVersions(topicName)

        then:
        wireMock.verify(1, deleteRequestedFor(versionsUrl(topicName)))
    }

    def "should throw exception on method not allowed response when deleting schema"() {
        given:
        wireMock.stubFor(WireMock.delete(versionsUrl(topicName)).willReturn(methodNotAllowedResponse()))

        when:
        client.deleteAllSchemaVersions(topicName)

        then:
        thrown(CouldNotRemoveSchemaException)
    }

    private UrlMatchingStrategy versionsUrl(TopicName topic) {
        urlEqualTo("/subjects/${topic.qualifiedName()}/versions")
    }

    private UrlMatchingStrategy schemaVersionUrl(TopicName topic, int version) {
        urlEqualTo("/subjects/${topic.qualifiedName()}/versions/$version")
    }

    private UrlMatchingStrategy schemaLatestVersionUrl(TopicName topic) {
        urlEqualTo("/subjects/${topic.qualifiedName()}/versions/latest")
    }

    private ResponseDefinitionBuilder okResponse() {
        aResponse().withStatus(200)
    }

    private ResponseDefinitionBuilder badRequestResponse() {
        aResponse().withStatus(400)
    }

    private ResponseDefinitionBuilder notFoundResponse() {
        aResponse().withStatus(404)
    }

    private ResponseDefinitionBuilder methodNotAllowedResponse() {
        aResponse().withStatus(405)
    }

    private ResponseDefinitionBuilder internalErrorResponse() {
        aResponse().withStatus(500)
    }
}
