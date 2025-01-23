package pl.allegro.tech.hermes.common.message.wrapper;

import jakarta.annotation.Nullable;
import pl.allegro.tech.hermes.api.Topic;

interface AvroMessageContentUnwrapper {

  AvroMessageContentUnwrapperResult unwrap(
      byte[] data, Topic topic, @Nullable Integer headerId, @Nullable Integer headerVersion);

  boolean isApplicable(byte[] data, Topic topic, Integer schemaId, Integer schemaVersion);
}
