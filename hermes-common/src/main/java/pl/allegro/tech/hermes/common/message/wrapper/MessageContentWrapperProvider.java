package pl.allegro.tech.hermes.common.message.wrapper;

import pl.allegro.tech.hermes.api.Topic;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class MessageContentWrapperProvider {

    private final Map<Topic.ContentType, MessageContentWrapper> wrappers = new HashMap<>(2);
    private final JsonMessageContentWrapper jsonMessageContentWrapper;

    @Inject
    public MessageContentWrapperProvider(JsonMessageContentWrapper jsonMessageContentWrapper,
                                         AvroMessageContentWrapper avroMessageContentWrapper) {

        this.jsonMessageContentWrapper = jsonMessageContentWrapper;

        wrappers.put(Topic.ContentType.JSON, jsonMessageContentWrapper);
        wrappers.put(Topic.ContentType.AVRO, avroMessageContentWrapper);
    }

    public MessageContentWrapper provide(Topic.ContentType contentType) {
        return wrappers.getOrDefault(contentType, jsonMessageContentWrapper);
    }
}
