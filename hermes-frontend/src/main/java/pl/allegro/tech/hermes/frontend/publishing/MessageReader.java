package pl.allegro.tech.hermes.frontend.publishing;

import com.codahale.metrics.Timer;
import com.google.common.base.Function;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Metrics;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static pl.allegro.tech.hermes.frontend.publishing.ContentLengthChecker.checkContentLength;
import static pl.allegro.tech.hermes.frontend.publishing.MessageState.State.PARSED;

public class MessageReader implements ReadListener {

    private final ByteArrayOutputStream messageContent = new ByteArrayOutputStream();
    private final ServletInputStream inputStream;
    private final Integer chunkSize;
    private final TopicName topicName;
    private final HermesMetrics hermesMetrics;
    private Timer.Context parsingTimerPerTopic;
    private Timer.Context parsingTimer;
    private HttpServletRequest request;
    private MessageState messageState;
    private Function<byte [], Void> onRead;
    private Function<IllegalStateException, Void> onValidationError;
    private Function<Throwable, Void> onOtherError;

    public MessageReader(
            HttpServletRequest request,
            Integer chunkSize,
            TopicName topicName,
            HermesMetrics hermesMetrics,
            MessageState messageState,
            Function<byte[], Void> onRead,
            Function<IllegalStateException, Void> onValidationError,
            Function<Throwable, Void> onOtherError) throws IOException {

        this.request = request;
        this.messageState = messageState;
        this.onValidationError = onValidationError;
        this.onOtherError = onOtherError;
        this.inputStream = request.getInputStream();
        this.chunkSize = chunkSize;
        this.topicName = topicName;
        this.hermesMetrics = hermesMetrics;
        this.onRead = onRead;

        messageState.setState(MessageState.State.PARSING);
        inputStream.setReadListener(this);
        initParsingTimers();
    }

    @Override
    public void onDataAvailable() throws IOException {
        int bufferLength;
        byte[] buffer = new byte[chunkSize];
        while (inputStream.isReady() && (bufferLength = inputStream.read(buffer)) != -1) {
            messageContent.write(buffer, 0, bufferLength);
        }
    }

    @Override
    public void onAllDataRead() {
        messageState.setState(PARSED);
        closeParsingTimers();
        try {
            checkContentLength(request, messageContent.size(), "Content-Length does not match the header");
            onRead.apply(messageContent.toByteArray());
        } catch (IllegalStateException e) {
            onValidationError.apply(e);
        }
    }

    @Override
    public void onError(Throwable t) {
        closeParsingTimers();
        onOtherError.apply(t);
    }

    private void initParsingTimers() {
        this.parsingTimerPerTopic = hermesMetrics.timer(Metrics.Timer.PRODUCER_PARSING_REQUEST, topicName).time();
        this.parsingTimer = hermesMetrics.timer(Metrics.Timer.PRODUCER_PARSING_REQUEST).time();
    }

    private void closeParsingTimers() {
        hermesMetrics.close(parsingTimer, parsingTimerPerTopic);
    }
}
