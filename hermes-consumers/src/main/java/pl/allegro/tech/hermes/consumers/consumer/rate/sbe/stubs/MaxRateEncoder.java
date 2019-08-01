/* Generated SBE (Simple Binary Encoding) message codec */
package pl.allegro.tech.hermes.consumers.consumer.rate.sbe.stubs;

import org.agrona.MutableDirectBuffer;
import org.agrona.DirectBuffer;

/**
 * Max-rates of a consumer node
 */
@SuppressWarnings("all")
public class MaxRateEncoder
{
    public static final int BLOCK_LENGTH = 0;
    public static final int TEMPLATE_ID = 1;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final MaxRateEncoder parentMessage = this;
    private MutableDirectBuffer buffer;
    protected int offset;
    protected int limit;

    public int sbeBlockLength()
    {
        return BLOCK_LENGTH;
    }

    public int sbeTemplateId()
    {
        return TEMPLATE_ID;
    }

    public int sbeSchemaId()
    {
        return SCHEMA_ID;
    }

    public int sbeSchemaVersion()
    {
        return SCHEMA_VERSION;
    }

    public String sbeSemanticType()
    {
        return "";
    }

    public MutableDirectBuffer buffer()
    {
        return buffer;
    }

    public int offset()
    {
        return offset;
    }

    public MaxRateEncoder wrap(final MutableDirectBuffer buffer, final int offset)
    {
        if (buffer != this.buffer)
        {
            this.buffer = buffer;
        }
        this.offset = offset;
        limit(offset + BLOCK_LENGTH);

        return this;
    }

    public MaxRateEncoder wrapAndApplyHeader(
        final MutableDirectBuffer buffer, final int offset, final MessageHeaderEncoder headerEncoder)
    {
        headerEncoder
            .wrap(buffer, offset)
            .blockLength(BLOCK_LENGTH)
            .templateId(TEMPLATE_ID)
            .schemaId(SCHEMA_ID)
            .version(SCHEMA_VERSION);

        return wrap(buffer, offset + MessageHeaderEncoder.ENCODED_LENGTH);
    }

    public int encodedLength()
    {
        return limit - offset;
    }

    public int limit()
    {
        return limit;
    }

    public void limit(final int limit)
    {
        this.limit = limit;
    }

    private final SubscriptionsEncoder subscriptions = new SubscriptionsEncoder(this);

    public static long subscriptionsId()
    {
        return 1;
    }

    public SubscriptionsEncoder subscriptionsCount(final int count)
    {
        subscriptions.wrap(buffer, count);
        return subscriptions;
    }

    public static class SubscriptionsEncoder
    {
        public static final int HEADER_SIZE = 4;
        private final MaxRateEncoder parentMessage;
        private MutableDirectBuffer buffer;
        private int count;
        private int index;
        private int offset;

        SubscriptionsEncoder(final MaxRateEncoder parentMessage)
        {
            this.parentMessage = parentMessage;
        }

        public void wrap(final MutableDirectBuffer buffer, final int count)
        {
            if (count < 0 || count > 65534)
            {
                throw new IllegalArgumentException("count outside allowed range: count=" + count);
            }

            if (buffer != this.buffer)
            {
                this.buffer = buffer;
            }

            index = -1;
            this.count = count;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + HEADER_SIZE);
            buffer.putShort(limit + 0, (short)(int)10, java.nio.ByteOrder.LITTLE_ENDIAN);
            buffer.putShort(limit + 2, (short)(int)count, java.nio.ByteOrder.LITTLE_ENDIAN);
        }

        public static int sbeHeaderSize()
        {
            return HEADER_SIZE;
        }

        public static int sbeBlockLength()
        {
            return 10;
        }

        public SubscriptionsEncoder next()
        {
            if (index + 1 >= count)
            {
                throw new java.util.NoSuchElementException();
            }

            offset = parentMessage.limit();
            parentMessage.limit(offset + sbeBlockLength());
            ++index;

            return this;
        }

        public static int idId()
        {
            return 2;
        }

        public static int idSinceVersion()
        {
            return 0;
        }

        public static int idEncodingOffset()
        {
            return 0;
        }

        public static int idEncodingLength()
        {
            return 2;
        }

        public static String idMetaAttribute(final MetaAttribute metaAttribute)
        {
            switch (metaAttribute)
            {
                case EPOCH: return "";
                case TIME_UNIT: return "";
                case SEMANTIC_TYPE: return "";
                case PRESENCE: return "required";
            }

            return "";
        }

        public static int idNullValue()
        {
            return 65535;
        }

        public static int idMinValue()
        {
            return 0;
        }

        public static int idMaxValue()
        {
            return 65534;
        }

        public SubscriptionsEncoder id(final int value)
        {
            buffer.putShort(offset + 0, (short)value, java.nio.ByteOrder.LITTLE_ENDIAN);
            return this;
        }


        public static int maxRateId()
        {
            return 3;
        }

        public static int maxRateSinceVersion()
        {
            return 0;
        }

        public static int maxRateEncodingOffset()
        {
            return 2;
        }

        public static int maxRateEncodingLength()
        {
            return 8;
        }

        public static String maxRateMetaAttribute(final MetaAttribute metaAttribute)
        {
            switch (metaAttribute)
            {
                case EPOCH: return "";
                case TIME_UNIT: return "";
                case SEMANTIC_TYPE: return "";
                case PRESENCE: return "required";
            }

            return "";
        }

        public static double maxRateNullValue()
        {
            return Double.NaN;
        }

        public static double maxRateMinValue()
        {
            return 4.9E-324d;
        }

        public static double maxRateMaxValue()
        {
            return 1.7976931348623157E308d;
        }

        public SubscriptionsEncoder maxRate(final double value)
        {
            buffer.putDouble(offset + 2, value, java.nio.ByteOrder.LITTLE_ENDIAN);
            return this;
        }

    }


    public String toString()
    {
        return appendTo(new StringBuilder(100)).toString();
    }

    public StringBuilder appendTo(final StringBuilder builder)
    {
        MaxRateDecoder writer = new MaxRateDecoder();
        writer.wrap(buffer, offset, BLOCK_LENGTH, SCHEMA_VERSION);

        return writer.appendTo(builder);
    }
}
