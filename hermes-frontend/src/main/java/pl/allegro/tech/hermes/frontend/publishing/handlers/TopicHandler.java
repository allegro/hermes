package pl.allegro.tech.hermes.frontend.publishing.handlers;

import static io.undertow.util.StatusCodes.INTERNAL_SERVER_ERROR;
import static pl.allegro.tech.hermes.api.ErrorCode.AUTH_ERROR;
import static pl.allegro.tech.hermes.api.ErrorCode.TOPIC_BLACKLISTED;
import static pl.allegro.tech.hermes.api.ErrorCode.TOPIC_NOT_EXISTS;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;

import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.util.Optional;
import java.util.function.Consumer;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;
import pl.allegro.tech.hermes.frontend.server.auth.Roles;

class TopicHandler implements HttpHandler {

  private static final String UNKNOWN_TOPIC_NAME = "unknown";

  private final HttpHandler next;
  private final TopicsCache topicsCache;
  private final MessageErrorProcessor messageErrorProcessor;

  TopicHandler(
      HttpHandler next, TopicsCache topicsCache, MessageErrorProcessor messageErrorProcessor) {
    this.next = next;
    this.topicsCache = topicsCache;
    this.messageErrorProcessor = messageErrorProcessor;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    if (exchange.isInIoThread()) {
      // switch to worker thread
      exchange.dispatch(this);
      return;
    }

    String messageId = MessageIdGenerator.generate();

    onRequestValid(
        exchange,
        messageId,
        cachedTopic -> {
          exchange.addExchangeCompleteListener(new ExchangeMetrics(cachedTopic));
          exchange.putAttachment(
              AttachmentContent.KEY,
              new AttachmentContent(cachedTopic, new MessageState(), messageId));
          setDefaultResponseCode(exchange);
          try {
            next.handleRequest(exchange);
          } catch (Exception e) {
            messageErrorProcessor.sendAndLog(exchange, cachedTopic.getTopic(), messageId, e);
          }
        });
  }

  private void onRequestValid(
      HttpServerExchange exchange, String messageId, Consumer<CachedTopic> consumer) {
    String topicName = exchange.getQueryParameters().get("qualifiedTopicName").getFirst();
    Optional<CachedTopic> maybeTopic = topicsCache.getTopic(topicName);

    if (!maybeTopic.isPresent()) {
      unknownTopic(exchange, topicName, messageId);
      return;
    }

    CachedTopic cachedTopic = maybeTopic.get();
    if (cachedTopic.isBlacklisted()) {
      blacklistedTopic(exchange, topicName, messageId);
      return;
    }

    Topic topic = cachedTopic.getTopic();
    if (topic.isAuthEnabled() && !hasPermission(exchange, topic)) {
      requestForbidden(exchange, messageId, topicName);
      return;
    }

    consumer.accept(cachedTopic);
  }

  private boolean hasPermission(HttpServerExchange exchange, Topic topic) {
    Optional<Account> account = extractAccount(exchange);
    return account
        .map(value -> hasPermission(topic, value))
        .orElseGet(topic::isUnauthenticatedAccessEnabled);
  }

  private boolean hasPermission(Topic topic, Account publisher) {
    return publisher.getRoles().contains(Roles.PUBLISHER)
        && topic.hasPermission(publisher.getPrincipal().getName());
  }

  private Optional<Account> extractAccount(HttpServerExchange exchange) {
    SecurityContext securityCtx = exchange.getSecurityContext();
    return Optional.ofNullable(securityCtx != null ? securityCtx.getAuthenticatedAccount() : null);
  }

  private void unknownTopic(
      HttpServerExchange exchange, String qualifiedTopicName, String messageId) {
    messageErrorProcessor.sendQuietly(
        exchange,
        error("Topic not found: " + qualifiedTopicName, TOPIC_NOT_EXISTS),
        messageId,
        UNKNOWN_TOPIC_NAME);
  }

  private void requestForbidden(
      HttpServerExchange exchange, String messageId, String qualifiedTopicName) {
    messageErrorProcessor.sendQuietly(
        exchange, error("Permission denied.", AUTH_ERROR), messageId, qualifiedTopicName);
  }

  private void blacklistedTopic(
      HttpServerExchange exchange, String qualifiedTopicName, String messageId) {
    messageErrorProcessor.sendQuietly(
        exchange,
        error("Topic blacklisted: " + qualifiedTopicName, TOPIC_BLACKLISTED),
        messageId,
        qualifiedTopicName);
  }

  // Default Undertow's response code (200) was changed in order to avoid situations in which
  // something wrong happens and Hermes-Frontend
  // does not publish message but return code 200
  // Since the default code is 500, clients have information that they should retry publishing
  private void setDefaultResponseCode(HttpServerExchange exchange) {
    exchange.setStatusCode(INTERNAL_SERVER_ERROR);
  }
}
