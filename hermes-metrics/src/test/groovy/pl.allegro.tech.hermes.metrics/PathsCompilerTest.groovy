package pl.allegro.tech.hermes.common.metric

import pl.allegro.tech.hermes.metrics.PathsCompiler
import spock.lang.Shared
import spock.lang.Specification

import static pl.allegro.tech.hermes.metrics.PathContext.pathContext
import static pl.allegro.tech.hermes.metrics.PathsCompiler.*

class PathsCompilerTest extends Specification {

    @Shared
    def pathsCompiler = new PathsCompiler("localhost.localdomain")

    def "should compile path with hostname"() {
        expect:
        pathsCompiler.compile(HOSTNAME + ".counter") == "localhost_localdomain.counter"
    }

    def "should compile path with path context"() {
        given:
        def pathContext = pathContext().withGroup("group")
                .withTopic("topic")
                .withSubscription("subscription")
                .withPartition(0)
                .withHttpCode(201)
                .build()

        when:
        def compiled = pathsCompiler.compile("hermes.$GROUP.$TOPIC.$SUBSCRIPTION.$PARTITION.$HTTP_CODE", pathContext)

        then:
        compiled == "hermes.group.topic.subscription.0.201"
    }
}
