package pl.allegro.tech.hermes.mock;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.google.common.collect.Lists;
import static java.util.stream.Collectors.toList;
import org.apache.avro.Schema;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class HermesMockQuery {
    private final HermesMockHelper hermesMockHelper;

    public HermesMockQuery(HermesMockHelper hermesMockHelper) {
        this.hermesMockHelper = hermesMockHelper;
    }

    public List<Request> allRequests() {
        return hermesMockHelper.findAll(postRequestedFor(urlPathMatching(("/topics/.*")))).stream()
                .map(Request::new)
                .collect(toList());
    }

    public List<Request> allRequestsOnTopic(String topicName) {
        RequestPatternBuilder matcher = postRequestedFor(urlEqualTo("/topics/" + topicName));
        return hermesMockHelper.findAll(matcher).stream()
                .map(Request::new)
                .collect(toList());
    }

    public <T> List<T> matchingJsonMessagesAs(String topicName, Class<T> clazz, Predicate<T> predicate) {
        return allRequestsOnTopic(topicName).stream()
                .map(req -> hermesMockHelper.deserializeJson(req.getBody(), clazz))
                .filter(predicate)
                .collect(toList());
    }

    public <T> List<T> allJsonMessagesAs(String topicName, Class<T> clazz) {
        return matchingJsonMessagesAs(topicName, clazz, t -> true);
    }

    public <T> long countMatchingJsonMessages(String topicName, Class<T> clazz, Predicate<T> predicate) {
        return allRequestsOnTopic(topicName).stream()
                .map(req -> hermesMockHelper.deserializeJson(req.getBody(), clazz))
                .filter(predicate)
                .count();
    }

    public <T> long countJsonMessages(String topicName, Class<T> clazz) {
        return countMatchingJsonMessages(topicName, clazz, t -> true);
    }

    public List<byte[]> allAvroRawMessages(String topicName) {
        return allRequestsOnTopic(topicName).stream()
                .map(Request::getBody)
                .collect(toList());
    }

    public <T> List<T> matchingAvroMessagesAs(String topicName, Schema schema, Class<T> clazz, Predicate<T> predicate) {
        return allRequestsOnTopic(topicName).stream()
                .map(req -> hermesMockHelper.deserializeAvro(req, schema, clazz))
                .filter(predicate)
                .collect(toList());
    }

    public <T> List<T> allAvroMessagesAs(String topicName, Schema schema, Class<T> clazz) {
        return matchingAvroMessagesAs(topicName, schema, clazz, t -> true);
    }

    public <T> long countMatchingAvroMessages(String topicName, Schema schema, Class<T> clazz, Predicate<T> predicate) {
        return allRequestsOnTopic(topicName).stream()
                .map(req -> hermesMockHelper.deserializeAvro(req, schema, clazz))
                .filter(predicate)
                .count();
    }

    public long countAvroMessages(String topicName) {
        return allRequestsOnTopic(topicName).stream()
                .map(Request::getBody)
                .count();
    }

    public Optional<Request> lastRequest(String topicName) {
        List<Request> matchingRequests = allRequestsOnTopic(topicName);
        return matchingRequests.isEmpty()
                ? Optional.empty()
                : Optional.of(matchingRequests.get(matchingRequests.size() - 1));
    }

    public <T> Optional<T> lastMatchingJsonMessageAs(String topicName, Class<T> clazz, Predicate<T> predicate) {
        return Lists.reverse(allJsonMessagesAs(topicName, clazz))
                .stream()
                .filter(predicate)
                .findFirst();
    }

    public <T> Optional<T> lastJsonMessageAs(String topicName, Class<T> clazz) {
        return lastRequest(topicName)
                .map(req -> hermesMockHelper.deserializeJson(req.getBody(), clazz));
    }

    public <T> Optional<T> lastMatchingAvroMessageAs(String topicName, Schema schema, Class<T> clazz, Predicate<T> predicate) {
        return Lists.reverse(allAvroMessagesAs(topicName, schema, clazz))
                .stream()
                .filter(predicate)
                .findFirst();
    }

    public Optional<byte[]> lastAvroRawMessage(String topicName) {
        return lastRequest(topicName)
                .map(Request::getBody);
    }

    public <T> Optional<T> lastAvroMessageAs(String topicName, Schema schema, Class<T> clazz) {
        return lastRequest(topicName)
                .map(req -> hermesMockHelper.deserializeAvro(req, schema, clazz));
    }
}
