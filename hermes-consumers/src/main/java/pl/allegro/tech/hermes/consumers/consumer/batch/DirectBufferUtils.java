package pl.allegro.tech.hermes.consumers.consumer.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

class DirectBufferUtils {
    private static final Logger logger = LoggerFactory.getLogger(DirectBufferUtils.class);

    private static final DirectBufferCleaner CLEANER;

    static {
        DirectBufferCleaner cleaner;
        try {
            cleaner = new Java11DirectBufferCleaner();
        } catch (ReflectiveOperationException e) {
            cleaner = null;
        }
        CLEANER = cleaner;
    }

    static void release(ByteBuffer buffer) {
        try {
            if (CLEANER != null && buffer.isDirect()) {
                CLEANER.clean(buffer);
            }
        } catch (ReflectiveOperationException e) {
            logger.warn("Releasing ByteBuffer failed", e);
        }
    }

    private interface DirectBufferCleaner {
        void clean(ByteBuffer buffer) throws ReflectiveOperationException;
    }

    static class Java11DirectBufferCleaner implements DirectBufferCleaner {
        private final Object unsafe;
        private final Method invokeCleaner;

        Java11DirectBufferCleaner() throws ReflectiveOperationException {
            Class<?> clazz = Class.forName("sun.misc.Unsafe");
            Field field = clazz.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = field.get(null);
            invokeCleaner = clazz.getMethod("invokeCleaner", ByteBuffer.class);
        }

        @Override
        public void clean(ByteBuffer buffer) throws ReflectiveOperationException {
            invokeCleaner.invoke(unsafe, buffer);
        }
    }
}
