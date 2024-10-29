package pl.allegro.tech.hermes.consumers.consumer.batch;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DirectBufferUtilsTest {
  @Test
  public void shouldReleaseDirectByteBuffer() {
    // given
    ByteBuffer buffer = ByteBuffer.allocateDirect(128);

    // when & then
    assertEquals(DirectBufferUtils.supportsReleasing(), true);
    Assertions.assertThatCode(() -> DirectBufferUtils.release(buffer)).doesNotThrowAnyException();
  }

  @Test
  public void shouldNotReleaseByteBuffer() {
    // given
    ByteBuffer buffer = ByteBuffer.allocate(128);

    // when & then
    assertEquals(DirectBufferUtils.supportsReleasing(), true);
    Assertions.assertThatCode(() -> DirectBufferUtils.release(buffer)).doesNotThrowAnyException();
  }
}
