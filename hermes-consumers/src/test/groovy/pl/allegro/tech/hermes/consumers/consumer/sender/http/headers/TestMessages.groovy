package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.Header
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.schema.CompiledSchema
import pl.allegro.tech.hermes.schema.SchemaVersion

class TestMessages {

    static Message message() {
        new Message("123", null, null, ContentType.JSON, Optional.empty(), 0l, 0l, null, 0l, Collections.emptyMap(), Collections.emptyList(), null, false)
    }

    static Message messageWithSubscriptionData() {
        new Message("123", "topic1", null, ContentType.JSON, Optional.empty(), 0l, 0l, null, 0l, Collections.emptyMap(), Collections.emptyList(), "subscription1", true)
    }

    static Message messageWithSchemaVersion() {
        new Message("123", null, null, ContentType.JSON, Optional.of(new CompiledSchema<>(1, SchemaVersion.valueOf(1))), 0l, 0l, null, 0l, Collections.emptyMap(), Collections.emptyList(), null, false)
    }

    static Message messageWithAdditionalHeaders() {
        new Message("123", null, null, ContentType.JSON, Optional.empty(), 0l, 0l, null, 0l, Collections.emptyMap(), Collections.singletonList(new Header("additional-header", "v")), null, false)
    }

}
