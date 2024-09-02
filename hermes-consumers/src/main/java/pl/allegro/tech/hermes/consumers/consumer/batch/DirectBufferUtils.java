package pl.allegro.tech.hermes.consumers.consumer.batch;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DirectBufferUtils {
  private static final Logger logger = LoggerFactory.getLogger(DirectBufferUtils.class);

  private static final DirectBufferCleaner CLEANER;

  static {
    DirectBufferCleaner cleaner;
    try {
      cleaner = new DirectBufferCleaner();
    } catch (ReflectiveOperationException e) {
      cleaner = null;
    }
    CLEANER = cleaner;
  }

  static boolean supportsReleasing() {
    return CLEANER != null;
  }

  static void release(ByteBuffer buffer) {
    try {
      if (supportsReleasing() && buffer.isDirect()) {
        CLEANER.clean(buffer);
      }
    } catch (ReflectiveOperationException e) {
      logger.warn("Releasing ByteBuffer failed", e);
    }
  }

  static class DirectBufferCleaner {
    private final Object unsafe;
    private final Method invokeCleaner;

    DirectBufferCleaner() throws ReflectiveOperationException {
      Class<?> clazz = Class.forName("sun.misc.Unsafe");
      Field field = clazz.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      unsafe = field.get(null);
      invokeCleaner = clazz.getMethod("invokeCleaner", ByteBuffer.class);
    }

    public void clean(ByteBuffer buffer) throws ReflectiveOperationException {
      invokeCleaner.invoke(unsafe, buffer);
    }
  }
}
