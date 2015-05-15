package pl.allegro.tech.hermes.common.util;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class MessageId {
    private static final HashFunction HASH_FUNCTION = Hashing.murmur3_128();

    public static String forTopicAndOffset(String topic, Long offset) {

        return HASH_FUNCTION.newHasher()
                .putString(topic, Charsets.UTF_8)
                .putLong(offset)
                .hash()
                .toString();

    }
}
