package pl.allegro.tech.hermes.common.message.wrapper;

import pl.allegro.tech.hermes.api.Topic;

import javax.inject.Inject;
import java.util.EnumMap;

public class MessageContentWrapperProvider {

    private final EnumMap<Topic.ContentType, MessageContentWrapper> wrappers = new EnumMap<>(Topic.ContentType.class);

    @Inject
    public MessageContentWrapperProvider(JsonMessageContentWrapper jsonMessageContentWrapper,
                                         AvroMessageContentWrapper avroMessageContentWrapper) {
        wrappers.put(Topic.ContentType.JSON, jsonMessageContentWrapper);
        wrappers.put(Topic.ContentType.AVRO, avroMessageContentWrapper);
    }

    public MessageContentWrapper provide(Topic.ContentType contentType) {
        return wrappers.get(contentType);
    }
}
