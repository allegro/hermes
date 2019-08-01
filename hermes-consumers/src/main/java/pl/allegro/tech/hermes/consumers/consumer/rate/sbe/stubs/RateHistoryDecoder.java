/* Generated SBE (Simple Binary Encoding) message codec */
package pl.allegro.tech.hermes.consumers.consumer.rate.sbe.stubs;

import org.agrona.MutableDirectBuffer;
import org.agrona.DirectBuffer;

/**
 * Historic rate of a consumer node
 */
@SuppressWarnings("all")
public class RateHistoryDecoder
{
    public static final int BLOCK_LENGTH = 0;
    public static final int TEMPLATE_ID = 2;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final RateHistoryDecoder parentMessage = this;
    private DirectBuffer buffer;
    protected int offset;
    protected int limit;
    protected int actingBlockLength;
    protected int actingVersion;

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

    public DirectBuffer buffer()
    {
        return buffer;
    }

    public int offset()
    {
        return offset;
    }

    public RateHistoryDecoder wrap(
        final DirectBuffer buffer, final int offset, final int actingBlockLength, final int actingVersion)
    {
        if (buffer != this.buffer)
        {
            this.buffer = buffer;
        }
        this.offset = offset;
        this.actingBlockLength = actingBlockLength;
        this.actingVersion = actingVersion;
        limit(offset + actingBlockLength);

        return this;
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

    private final SubscriptionsDecoder subscriptions = new SubscriptionsDecoder(this);

    public static long subscriptionsDecoderId()
    {
        return 1;
    }

    public static int subscriptionsDecoderSinceVersion()
    {
        return 0;
    }

    public SubscriptionsDecoder subscriptions()
    {
        subscriptions.wrap(buffer);
        return subscriptions;
    }

    public static class SubscriptionsDecoder
        implements Iterable<SubscriptionsDecoder>, java.util.Iterator<SubscriptionsDecoder>
    {
        public static final int HEADER_SIZE = 4;
        private final RateHistoryDecoder parentMessage;
        private DirectBuffer buffer;
        private int count;
        private int index;
        private int offset;
        private int blockLength;
        private final RatesDecoder rates;

        SubscriptionsDecoder(final RateHistoryDecoder parentMessage)
        {
            this.parentMessage = parentMessage;
            rates = new RatesDecoder(parentMessage);
        }

        public void wrap(final DirectBuffer buffer)
        {
            if (buffer != this.buffer)
            {
                this.buffer = buffer;
            }
            index = -1;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + HEADER_SIZE);
            blockLength = (int)(buffer.getShort(limit + 0, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF);
            count = (int)(buffer.getShort(limit + 2, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF);
        }

        public static int sbeHeaderSize()
        {
            return HEADER_SIZE;
        }

        public static int sbeBlockLength()
        {
            return 2;
        }

        public int actingBlockLength()
        {
            return blockLength;
        }

        public int count()
        {
            return count;
        }

        public java.util.Iterator<SubscriptionsDecoder> iterator()
        {
            return this;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        public boolean hasNext()
        {
            return (index + 1) < count;
        }

        public SubscriptionsDecoder next()
        {
            if (index + 1 >= count)
            {
                throw new java.util.NoSuchElementException();
            }

            offset = parentMessage.limit();
            parentMessage.limit(offset + blockLength);
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

        public int id()
        {
            return (buffer.getShort(offset + 0, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF);
        }


        public static long ratesDecoderId()
        {
            return 3;
        }

        public static int ratesDecoderSinceVersion()
        {
            return 0;
        }

        public RatesDecoder rates()
        {
            rates.wrap(buffer);
            return rates;
        }

        public static class RatesDecoder
            implements Iterable<RatesDecoder>, java.util.Iterator<RatesDecoder>
        {
            public static final int HEADER_SIZE = 4;
            private final RateHistoryDecoder parentMessage;
            private DirectBuffer buffer;
            private int count;
            private int index;
            private int offset;
            private int blockLength;

            RatesDecoder(final RateHistoryDecoder parentMessage)
            {
                this.parentMessage = parentMessage;
            }

            public void wrap(final DirectBuffer buffer)
            {
                if (buffer != this.buffer)
                {
                    this.buffer = buffer;
                }
                index = -1;
                final int limit = parentMessage.limit();
                parentMessage.limit(limit + HEADER_SIZE);
                blockLength = (int)(buffer.getShort(limit + 0, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF);
                count = (int)(buffer.getShort(limit + 2, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF);
            }

            public static int sbeHeaderSize()
            {
                return HEADER_SIZE;
            }

            public static int sbeBlockLength()
            {
                return 8;
            }

            public int actingBlockLength()
            {
                return blockLength;
            }

            public int count()
            {
                return count;
            }

            public java.util.Iterator<RatesDecoder> iterator()
            {
                return this;
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext()
            {
                return (index + 1) < count;
            }

            public RatesDecoder next()
            {
                if (index + 1 >= count)
                {
                    throw new java.util.NoSuchElementException();
                }

                offset = parentMessage.limit();
                parentMessage.limit(offset + blockLength);
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

            public double rate()
            {
                return buffer.getDouble(offset + 0, java.nio.ByteOrder.LITTLE_ENDIAN);
            }



            public String toString()
            {
                return appendTo(new StringBuilder(100)).toString();
            }

            public StringBuilder appendTo(final StringBuilder builder)
            {
                builder.append('(');
                //Token{signal=BEGIN_FIELD, name='Rate', referencedName='null', description='null', id=4, version=0, deprecated=0, encodedLength=8, offset=0, componentTokenCount=3, encoding=Encoding{presence=REQUIRED, primitiveType=null, byteOrder=LITTLE_ENDIAN, minValue=null, maxValue=null, nullValue=null, constValue=null, characterEncoding='null', epoch='null', timeUnit=null, semanticType='null'}}
                //Token{signal=ENCODING, name='double', referencedName='null', description='null', id=-1, version=0, deprecated=0, encodedLength=8, offset=0, componentTokenCount=1, encoding=Encoding{presence=REQUIRED, primitiveType=DOUBLE, byteOrder=LITTLE_ENDIAN, minValue=null, maxValue=null, nullValue=null, constValue=null, characterEncoding='null', epoch='null', timeUnit=null, semanticType='null'}}
                builder.append("rate=");
                builder.append(rate());
                builder.append(')');
                return builder;
            }
        }


        public String toString()
        {
            return appendTo(new StringBuilder(100)).toString();
        }

        public StringBuilder appendTo(final StringBuilder builder)
        {
            builder.append('(');
            //Token{signal=BEGIN_FIELD, name='Id', referencedName='null', description='null', id=2, version=0, deprecated=0, encodedLength=2, offset=0, componentTokenCount=3, encoding=Encoding{presence=REQUIRED, primitiveType=null, byteOrder=LITTLE_ENDIAN, minValue=null, maxValue=null, nullValue=null, constValue=null, characterEncoding='null', epoch='null', timeUnit=null, semanticType='null'}}
            //Token{signal=ENCODING, name='uint16', referencedName='null', description='null', id=-1, version=0, deprecated=0, encodedLength=2, offset=0, componentTokenCount=1, encoding=Encoding{presence=REQUIRED, primitiveType=UINT16, byteOrder=LITTLE_ENDIAN, minValue=null, maxValue=null, nullValue=null, constValue=null, characterEncoding='null', epoch='null', timeUnit=null, semanticType='null'}}
            builder.append("id=");
            builder.append(id());
            builder.append('|');
            //Token{signal=BEGIN_GROUP, name='Rates', referencedName='null', description='null', id=3, version=0, deprecated=0, encodedLength=8, offset=2, componentTokenCount=9, encoding=Encoding{presence=REQUIRED, primitiveType=null, byteOrder=LITTLE_ENDIAN, minValue=null, maxValue=null, nullValue=null, constValue=null, characterEncoding='null', epoch='null', timeUnit=null, semanticType='null'}}
            builder.append("rates=[");
            RatesDecoder rates = rates();
            if (rates.count() > 0)
            {
                while (rates.hasNext())
                {
                    rates.next().appendTo(builder);
                    builder.append(',');
                }
                builder.setLength(builder.length() - 1);
            }
            builder.append(']');
            builder.append(')');
            return builder;
        }
    }


    public String toString()
    {
        return appendTo(new StringBuilder(100)).toString();
    }

    public StringBuilder appendTo(final StringBuilder builder)
    {
        final int originalLimit = limit();
        limit(offset + actingBlockLength);
        builder.append("[RateHistory](sbeTemplateId=");
        builder.append(TEMPLATE_ID);
        builder.append("|sbeSchemaId=");
        builder.append(SCHEMA_ID);
        builder.append("|sbeSchemaVersion=");
        if (parentMessage.actingVersion != SCHEMA_VERSION)
        {
            builder.append(parentMessage.actingVersion);
            builder.append('/');
        }
        builder.append(SCHEMA_VERSION);
        builder.append("|sbeBlockLength=");
        if (actingBlockLength != BLOCK_LENGTH)
        {
            builder.append(actingBlockLength);
            builder.append('/');
        }
        builder.append(BLOCK_LENGTH);
        builder.append("):");
        //Token{signal=BEGIN_GROUP, name='Subscriptions', referencedName='null', description='null', id=1, version=0, deprecated=0, encodedLength=2, offset=0, componentTokenCount=18, encoding=Encoding{presence=REQUIRED, primitiveType=null, byteOrder=LITTLE_ENDIAN, minValue=null, maxValue=null, nullValue=null, constValue=null, characterEncoding='null', epoch='null', timeUnit=null, semanticType='null'}}
        builder.append("subscriptions=[");
        SubscriptionsDecoder subscriptions = subscriptions();
        if (subscriptions.count() > 0)
        {
            while (subscriptions.hasNext())
            {
                subscriptions.next().appendTo(builder);
                builder.append(',');
            }
            builder.setLength(builder.length() - 1);
        }
        builder.append(']');

        limit(originalLimit);

        return builder;
    }
}
