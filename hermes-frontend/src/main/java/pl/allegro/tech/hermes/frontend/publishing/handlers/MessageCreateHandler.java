package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.common.message.wrapper.SchemaMissingException;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageFactory;
import pl.allegro.tech.hermes.frontend.validator.InvalidMessageException;
import pl.allegro.tech.hermes.schema.CouldNotLoadSchemaException;
import tech.allegro.schema.json2avro.converter.AvroConversionException;

import static pl.allegro.tech.hermes.api.ErrorCode.INTERNAL_ERROR;
import static pl.allegro.tech.hermes.api.ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
import static pl.allegro.tech.hermes.api.ErrorCode.VALIDATION_ERROR;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;

class MessageCreateHandler implements HttpHandler {

    private final HttpHandler next;
    private final MessageFactory messageFactory;
    private final MessageErrorProcessor messageErrorProcessor;

    MessageCreateHandler(HttpHandler next, MessageFactory messageFactory, MessageErrorProcessor messageErrorProcessor) {
        this.next = next;
        this.messageFactory = messageFactory;
        this.messageErrorProcessor = messageErrorProcessor;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);

        try {
            attachment.setMessage(messageFactory.create(exchange.getRequestHeaders(), attachment));
            next.handleRequest(exchange);
        } catch (InvalidMessageException | AvroConversionException | UnsupportedContentTypeException exception) {
            attachment.removeTimeout();
            messageErrorProcessor.sendAndLog(
                    exchange,
                    attachment.getTopic(),
                    attachment.getMessageId(),
                    error("Invalid message", VALIDATION_ERROR),
                    exception);
        } catch (CouldNotLoadSchemaException | SchemaMissingException exception) {
            attachment.removeTimeout();
            messageErrorProcessor.sendAndLog(
                    exchange,
                    attachment.getTopic(),
                    attachment.getMessageId(),
                    error("Missing schema", SCHEMA_COULD_NOT_BE_LOADED),
                    exception);
        } catch (Exception exception) {
            attachment.removeTimeout();
            messageErrorProcessor.sendAndLog(
                    exchange,
                    attachment.getTopic(),
                    attachment.getMessageId(),
                    error("Exception caught while creating message", INTERNAL_ERROR),
                    exception);
        }
    }

}
