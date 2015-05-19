package pl.allegro.tech.hermes.common.metric

import spock.lang.Shared
import spock.lang.Specification

import static PathsCompiler.$GROUP
import static PathsCompiler.$HOSTNAME
import static PathsCompiler.$HTTP_CODE
import static PathsCompiler.$PARTITION
import static PathsCompiler.$SUBSCRIPTION
import static PathsCompiler.$TOPIC
import static pl.allegro.tech.hermes.common.metric.PathContext.pathContext


class PathsCompilerTest extends Specification {

    @Shared
    def pathsCompiler = new PathsCompiler("localhost")

    def "should compile path with hostname"() {
        expect:
        pathsCompiler.compile($HOSTNAME + ".counter") == "localhost.counter"
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
        def compiled = pathsCompiler.compile("hermes." + $GROUP + "." + $TOPIC + "." + $SUBSCRIPTION + "." + $PARTITION + "." + $HTTP_CODE,
                pathContext)

        then:
        compiled == "hermes.group.topic.subscription.0.201"
    }
}
