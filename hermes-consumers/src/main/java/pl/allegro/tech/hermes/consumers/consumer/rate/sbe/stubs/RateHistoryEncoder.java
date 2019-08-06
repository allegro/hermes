/* Generated SBE (Simple Binary Encoding) message codec */
package pl.allegro.tech.hermes.consumers.consumer.rate.sbe.stubs;

import org.agrona.MutableDirectBuffer;
import org.agrona.DirectBuffer;

/**
 * Historic rate of a consumer node
 */
@SuppressWarnings("all")
public class RateHistoryEncoder
{
    public static final int BLOCK_LENGTH = 0;
    public static final int TEMPLATE_ID = 2;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final RateHistoryEncoder parentMessage = this;
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

    public RateHistoryEncoder wrap(final MutableDirectBuffer buffer, final int offset)
    {
        if (buffer != this.buffer)
        {
            this.buffer = buffer;
        }
        this.offset = offset;
        limit(offset + BLOCK_LENGTH);

        return this;
    }

    public RateHistoryEncoder wrapAndApplyHeader(
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
        private final RateHistoryEncoder parentMessage;
        private MutableDirectBuffer buffer;
        private int count;
        private int index;
        private int offset;
        private final RatesEncoder rates;

        SubscriptionsEncoder(final RateHistoryEncoder parentMessage)
        {
            this.parentMessage = parentMessage;
            rates = new RatesEncoder(parentMessage);
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
            buffer.putShort(limit + 0, (short)(int)8, java.nio.ByteOrder.LITTLE_ENDIAN);
            buffer.putShort(limit + 2, (short)(int)count, java.nio.ByteOrder.LITTLE_ENDIAN);
        }

        public static int sbeHeaderSize()
        {
            return HEADER_SIZE;
        }

        public static int sbeBlockLength()
        {
            return 8;
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
            return 8;
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

        public static long idNullValue()
        {
            return -9223372036854775808L;
        }

        public static long idMinValue()
        {
            return -9223372036854775807L;
        }

        public static long idMaxValue()
        {
            return 9223372036854775807L;
        }

        public SubscriptionsEncoder id(final long value)
        {
            buffer.putLong(offset + 0, value, java.nio.ByteOrder.LITTLE_ENDIAN);
            return this;
        }


        public static long ratesId()
        {
            return 3;
        }

        public RatesEncoder ratesCount(final int count)
        {
            rates.wrap(buffer, count);
            return rates;
        }

        public static class RatesEncoder
        {
            public static final int HEADER_SIZE = 4;
            private final RateHistoryEncoder parentMessage;
            private MutableDirectBuffer buffer;
            private int count;
            private int index;
            private int offset;

            RatesEncoder(final RateHistoryEncoder parentMessage)
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
                buffer.putShort(limit + 0, (short)(int)8, java.nio.ByteOrder.LITTLE_ENDIAN);
                buffer.putShort(limit + 2, (short)(int)count, java.nio.ByteOrder.LITTLE_ENDIAN);
            }

            public static int sbeHeaderSize()
            {
                return HEADER_SIZE;
            }

            public static int sbeBlockLength()
            {
                return 8;
            }

            public RatesEncoder next()
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

            public static int rateId()
            {
                return 4;
            }

            public static int rateSinceVersion()
            {
                return 0;
            }

            public static int rateEncodingOffset()
            {
                return 0;
            }

            public static int rateEncodingLength()
            {
                return 8;
            }

            public static String rateMetaAttribute(final MetaAttribute metaAttribute)
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

            public static double rateNullValue()
            {
                return Double.NaN;
            }

            public static double rateMinValue()
            {
                return 4.9E-324d;
            }

            public static double rateMaxValue()
            {
                return 1.7976931348623157E308d;
            }

            public RatesEncoder rate(final double value)
            {
                buffer.putDouble(offset + 0, value, java.nio.ByteOrder.LITTLE_ENDIAN);
                return this;
            }

        }
    }


    public String toString()
    {
        return appendTo(new StringBuilder(100)).toString();
    }

    public StringBuilder appendTo(final StringBuilder builder)
    {
        RateHistoryDecoder writer = new RateHistoryDecoder();
        writer.wrap(buffer, offset, BLOCK_LENGTH, SCHEMA_VERSION);

        return writer.appendTo(builder);
    }
}
