package pl.allegro.tech.hermes.frontend.publishing;

import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

import static pl.allegro.tech.hermes.frontend.publishing.ContentLengthChecker.checkContentLength;
import static pl.allegro.tech.hermes.frontend.publishing.message.MessageState.State.PARSED;

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
    private Consumer<byte []> onRead;
    private Consumer<IllegalStateException> onValidationError;
    private Consumer<Throwable> onOtherError;

    static public void startReadingMessage(HttpServletRequest request,
                                           Integer chunkSize,
                                           TopicName topicName,
                                           HermesMetrics hermesMetrics,
                                           MessageState messageState,
                                           Consumer<byte[]> onRead,
                                           Consumer<IllegalStateException> onValidationError,
                                           Consumer<Throwable> onOtherError) throws IOException {
        new MessageReader(request, chunkSize, topicName, hermesMetrics, messageState, onRead, onValidationError,
                onOtherError).startReading();
    }

    private MessageReader(
            HttpServletRequest request,
            Integer chunkSize,
            TopicName topicName,
            HermesMetrics hermesMetrics,
            MessageState messageState,
            Consumer<byte[]> onRead,
            Consumer<IllegalStateException> onValidationError,
            Consumer<Throwable> onOtherError) throws IOException {

        this.request = request;
        this.messageState = messageState;
        this.onValidationError = onValidationError;
        this.onOtherError = onOtherError;
        this.inputStream = request.getInputStream();
        this.chunkSize = chunkSize;
        this.topicName = topicName;
        this.hermesMetrics = hermesMetrics;
        this.onRead = onRead;
    }

    private void startReading () {
        messageState.setState(MessageState.State.PARSING);
        initParsingTimers();
        inputStream.setReadListener(this);
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
            hermesMetrics.reportContentSize(messageContent.size(), topicName);
            onRead.accept(messageContent.toByteArray());
        } catch (IllegalStateException e) {
            onValidationError.accept(e);
        }
    }

    @Override
    public void onError(Throwable t) {
        closeParsingTimers();
        onOtherError.accept(t);
    }

    private void initParsingTimers() {
        this.parsingTimerPerTopic = hermesMetrics.timer(Timers.TOPIC_PARSING_REQUEST, topicName).time();
        this.parsingTimer = hermesMetrics.timer(Timers.PARSING_REQUEST).time();
    }

    private void closeParsingTimers() {
        hermesMetrics.close(parsingTimer, parsingTimerPerTopic);
    }
}
