package pl.allegro.tech.hermes.frontend.buffer;

import org.junit.Test;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapEntryValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test protects against unintended changes to ChronicleMapEntryValue class. As ChronicleMap uses Java serialization
 * when writing the class to offheap (and to the file), any change in canonical class name breaks the compatibility
 * of buffers. Incompatible buffer will not be read from disk, which might lead to losing some messages between the
 * updates.
 */
public class BufferSerializationCompatibilityTest {

    @Test
    public void shouldKeepChronicleMapEntryValueClassCanonicalNameConsistent() {
        // then
        assertThat(ChronicleMapEntryValue.class.getCanonicalName())
                .isEqualTo("pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapEntryValue");
    }

}
