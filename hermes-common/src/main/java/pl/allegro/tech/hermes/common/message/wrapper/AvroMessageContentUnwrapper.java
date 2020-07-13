package pl.allegro.tech.hermes.common.message.wrapper;

import pl.allegro.tech.hermes.api.Topic;

import javax.annotation.Nullable;

interface AvroMessageContentUnwrapper {

    AvroMessageContentUnwrapperResult unwrap(byte[] data, Topic topic, @Nullable Integer headerId, @Nullable Integer headerVersion);

    boolean isApplicable(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion);
}
