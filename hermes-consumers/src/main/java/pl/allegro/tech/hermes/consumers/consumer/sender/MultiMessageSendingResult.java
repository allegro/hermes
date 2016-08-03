package pl.allegro.tech.hermes.consumers.consumer.sender;


import com.google.common.collect.ImmutableList;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class MultiMessageSendingResult implements MessageSendingResult {

    private final List<SingleMessageSendingResult> children;

    public MultiMessageSendingResult(List<SingleMessageSendingResult> children) {
        this.children = ImmutableList.copyOf(children);
    }

    @Override
    public int getStatusCode() {
        if (succeeded()) {
            return children.stream().mapToInt(MessageSendingResult::getStatusCode).max().orElse(0);
        } else {
            return children.stream().filter(child -> !child.succeeded()).mapToInt(MessageSendingResult::getStatusCode).max().orElse(0);
        }
    }

    @Override
    public boolean isLoggable() {
        return children.stream().anyMatch(MessageSendingResult::isLoggable);
    }

    @Override
    public boolean ignoreInRateCalculation(boolean retryClientErrors, boolean isOAuthSecuredSubscription) {
        return children.stream().allMatch(r -> r.ignoreInRateCalculation(retryClientErrors, isOAuthSecuredSubscription));
    }

    @Override
    public Optional<Long> getRetryAfterMillis() {
        return children.stream()
                .map(MessageSendingResult::getRetryAfterMillis)
                .filter(Optional::isPresent).map(Optional::get)
                .min(Comparator.naturalOrder());
    }

    @Override
    public boolean succeeded() {
        return !children.isEmpty() && children.stream().allMatch(MessageSendingResult::succeeded);
    }

    @Override
    public boolean isClientError() {
        List<SingleMessageSendingResult> failed = children.stream().filter(child -> !child.succeeded()).collect(Collectors.toList());
        return !failed.isEmpty() &&
                failed.stream().allMatch(MessageSendingResult::isClientError);
    }

    @Override
    public boolean isTimeout() {
        return !children.isEmpty() && children.stream().anyMatch(MessageSendingResult::isTimeout);
    }

    @Override
    public boolean isRetryLater() {
        return children.isEmpty() || children.stream().anyMatch(MessageSendingResult::isRetryLater);
    }

    @Override
    public List<MessageSendingResultLogInfo> getLogInfo() {
            return children.stream().map(child ->
                    new MessageSendingResultLogInfo(child.getRequestUri(), child.getFailure(), child.getRootCause()))
                    .collect(Collectors.toList());

    }

    @Override
    public List<String> getSucceededUris(Predicate<MessageSendingResult> filter) {
            return children.stream()
                    .filter(filter)
                    .map(SingleMessageSendingResult::getRequestUri)
                    .collect(Collectors.toList());

    }

    public List<SingleMessageSendingResult> getChildren() {
        return children;
    }

    @Override
    public String getRootCause() {
        return children.stream()
                .map(child -> child.getRequestUri()+":"+child.getRootCause())
                .collect(joining(";"));
    }
}

