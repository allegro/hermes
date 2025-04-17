package pl.allegro.tech.hermes.consumers.consumer.dead;

public final class DeadLetterMessageProtoRecord {
    private DeadLetterMessageProtoRecord() {}
    public static void registerAllExtensions(
            com.google.protobuf.ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(
            com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions(
                (com.google.protobuf.ExtensionRegistryLite) registry);
    }
    public interface DeadlettermessagerecordOrBuilder extends
            // @@protoc_insertion_point(interface_extends:pl.allegro.tech.hermes.consumers.consumer.dead.Deadlettermessagerecord)
            com.google.protobuf.MessageOrBuilder {

        /**
         * <code>string message_id = 1;</code>
         * @return The messageId.
         */
        java.lang.String getMessageId();
        /**
         * <code>string message_id = 1;</code>
         * @return The bytes for messageId.
         */
        com.google.protobuf.ByteString
        getMessageIdBytes();

        /**
         * <code>string batch_id = 2;</code>
         * @return The batchId.
         */
        java.lang.String getBatchId();
        /**
         * <code>string batch_id = 2;</code>
         * @return The bytes for batchId.
         */
        com.google.protobuf.ByteString
        getBatchIdBytes();

        /**
         * <code>int64 offset = 3;</code>
         * @return The offset.
         */
        long getOffset();

        /**
         * <code>int64 partition = 4;</code>
         * @return The partition.
         */
        long getPartition();

        /**
         * <code>int64 partition_assignment_term = 5;</code>
         * @return The partitionAssignmentTerm.
         */
        long getPartitionAssignmentTerm();

        /**
         * <code>string topic = 6;</code>
         * @return The topic.
         */
        java.lang.String getTopic();
        /**
         * <code>string topic = 6;</code>
         * @return The bytes for topic.
         */
        com.google.protobuf.ByteString
        getTopicBytes();

        /**
         * <code>string kafka_topic = 7;</code>
         * @return The kafkaTopic.
         */
        java.lang.String getKafkaTopic();
        /**
         * <code>string kafka_topic = 7;</code>
         * @return The bytes for kafkaTopic.
         */
        com.google.protobuf.ByteString
        getKafkaTopicBytes();

        /**
         * <code>string subscription = 8;</code>
         * @return The subscription.
         */
        java.lang.String getSubscription();
        /**
         * <code>string subscription = 8;</code>
         * @return The bytes for subscription.
         */
        com.google.protobuf.ByteString
        getSubscriptionBytes();

        /**
         * <code>int64 publishing_timestamp = 9;</code>
         * @return The publishingTimestamp.
         */
        long getPublishingTimestamp();

        /**
         * <code>int64 reading_timestamp = 10;</code>
         * @return The readingTimestamp.
         */
        long getReadingTimestamp();

        /**
         * <code>bytes body = 11;</code>
         * @return The body.
         */
        com.google.protobuf.ByteString getBody();
    }
    /**
     * Protobuf type {@code pl.allegro.tech.hermes.consumers.consumer.dead.Deadlettermessagerecord}
     */
    public static final class Deadlettermessagerecord extends
            com.google.protobuf.GeneratedMessageV3 implements
            // @@protoc_insertion_point(message_implements:pl.allegro.tech.hermes.consumers.consumer.dead.Deadlettermessagerecord)
            DeadlettermessagerecordOrBuilder {
        private static final long serialVersionUID = 0L;
        // Use Deadlettermessagerecord.newBuilder() to construct.
        private Deadlettermessagerecord(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
            super(builder);
        }
        private Deadlettermessagerecord() {
            messageId_ = "";
            batchId_ = "";
            topic_ = "";
            kafkaTopic_ = "";
            subscription_ = "";
            body_ = com.google.protobuf.ByteString.EMPTY;
        }

        @java.lang.Override
        @SuppressWarnings({"unused"})
        protected java.lang.Object newInstance(
                UnusedPrivateParameter unused) {
            return new Deadlettermessagerecord();
        }

        @java.lang.Override
        public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
            return this.unknownFields;
        }
        private Deadlettermessagerecord(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            this();
            if (extensionRegistry == null) {
                throw new java.lang.NullPointerException();
            }
            com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                    com.google.protobuf.UnknownFieldSet.newBuilder();
            try {
                boolean done = false;
                while (!done) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            done = true;
                            break;
                        case 10: {
                            java.lang.String s = input.readStringRequireUtf8();

                            messageId_ = s;
                            break;
                        }
                        case 18: {
                            java.lang.String s = input.readStringRequireUtf8();

                            batchId_ = s;
                            break;
                        }
                        case 24: {

                            offset_ = input.readInt64();
                            break;
                        }
                        case 32: {

                            partition_ = input.readInt64();
                            break;
                        }
                        case 40: {

                            partitionAssignmentTerm_ = input.readInt64();
                            break;
                        }
                        case 50: {
                            java.lang.String s = input.readStringRequireUtf8();

                            topic_ = s;
                            break;
                        }
                        case 58: {
                            java.lang.String s = input.readStringRequireUtf8();

                            kafkaTopic_ = s;
                            break;
                        }
                        case 66: {
                            java.lang.String s = input.readStringRequireUtf8();

                            subscription_ = s;
                            break;
                        }
                        case 72: {

                            publishingTimestamp_ = input.readInt64();
                            break;
                        }
                        case 80: {

                            readingTimestamp_ = input.readInt64();
                            break;
                        }
                        case 90: {

                            body_ = input.readBytes();
                            break;
                        }
                        default: {
                            if (!parseUnknownField(
                                    input, unknownFields, extensionRegistry, tag)) {
                                done = true;
                            }
                            break;
                        }
                    }
                }
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw e.setUnfinishedMessage(this);
            } catch (com.google.protobuf.UninitializedMessageException e) {
                throw e.asInvalidProtocolBufferException().setUnfinishedMessage(this);
            } catch (java.io.IOException e) {
                throw new com.google.protobuf.InvalidProtocolBufferException(
                        e).setUnfinishedMessage(this);
            } finally {
                this.unknownFields = unknownFields.build();
                makeExtensionsImmutable();
            }
        }
        public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
            return pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.internal_static_pl_allegro_tech_hermes_consumers_consumer_dead_Deadlettermessagerecord_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
            return pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.internal_static_pl_allegro_tech_hermes_consumers_consumer_dead_Deadlettermessagerecord_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(
                            pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord.class, pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord.Builder.class);
        }

        public static final int MESSAGE_ID_FIELD_NUMBER = 1;
        private volatile java.lang.Object messageId_;
        /**
         * <code>string message_id = 1;</code>
         * @return The messageId.
         */
        @java.lang.Override
        public java.lang.String getMessageId() {
            java.lang.Object ref = messageId_;
            if (ref instanceof java.lang.String) {
                return (java.lang.String) ref;
            } else {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                messageId_ = s;
                return s;
            }
        }
        /**
         * <code>string message_id = 1;</code>
         * @return The bytes for messageId.
         */
        @java.lang.Override
        public com.google.protobuf.ByteString
        getMessageIdBytes() {
            java.lang.Object ref = messageId_;
            if (ref instanceof java.lang.String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                messageId_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        public static final int BATCH_ID_FIELD_NUMBER = 2;
        private volatile java.lang.Object batchId_;
        /**
         * <code>string batch_id = 2;</code>
         * @return The batchId.
         */
        @java.lang.Override
        public java.lang.String getBatchId() {
            java.lang.Object ref = batchId_;
            if (ref instanceof java.lang.String) {
                return (java.lang.String) ref;
            } else {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                batchId_ = s;
                return s;
            }
        }
        /**
         * <code>string batch_id = 2;</code>
         * @return The bytes for batchId.
         */
        @java.lang.Override
        public com.google.protobuf.ByteString
        getBatchIdBytes() {
            java.lang.Object ref = batchId_;
            if (ref instanceof java.lang.String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                batchId_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        public static final int OFFSET_FIELD_NUMBER = 3;
        private long offset_;
        /**
         * <code>int64 offset = 3;</code>
         * @return The offset.
         */
        @java.lang.Override
        public long getOffset() {
            return offset_;
        }

        public static final int PARTITION_FIELD_NUMBER = 4;
        private long partition_;
        /**
         * <code>int64 partition = 4;</code>
         * @return The partition.
         */
        @java.lang.Override
        public long getPartition() {
            return partition_;
        }

        public static final int PARTITION_ASSIGNMENT_TERM_FIELD_NUMBER = 5;
        private long partitionAssignmentTerm_;
        /**
         * <code>int64 partition_assignment_term = 5;</code>
         * @return The partitionAssignmentTerm.
         */
        @java.lang.Override
        public long getPartitionAssignmentTerm() {
            return partitionAssignmentTerm_;
        }

        public static final int TOPIC_FIELD_NUMBER = 6;
        private volatile java.lang.Object topic_;
        /**
         * <code>string topic = 6;</code>
         * @return The topic.
         */
        @java.lang.Override
        public java.lang.String getTopic() {
            java.lang.Object ref = topic_;
            if (ref instanceof java.lang.String) {
                return (java.lang.String) ref;
            } else {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                topic_ = s;
                return s;
            }
        }
        /**
         * <code>string topic = 6;</code>
         * @return The bytes for topic.
         */
        @java.lang.Override
        public com.google.protobuf.ByteString
        getTopicBytes() {
            java.lang.Object ref = topic_;
            if (ref instanceof java.lang.String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                topic_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        public static final int KAFKA_TOPIC_FIELD_NUMBER = 7;
        private volatile java.lang.Object kafkaTopic_;
        /**
         * <code>string kafka_topic = 7;</code>
         * @return The kafkaTopic.
         */
        @java.lang.Override
        public java.lang.String getKafkaTopic() {
            java.lang.Object ref = kafkaTopic_;
            if (ref instanceof java.lang.String) {
                return (java.lang.String) ref;
            } else {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                kafkaTopic_ = s;
                return s;
            }
        }
        /**
         * <code>string kafka_topic = 7;</code>
         * @return The bytes for kafkaTopic.
         */
        @java.lang.Override
        public com.google.protobuf.ByteString
        getKafkaTopicBytes() {
            java.lang.Object ref = kafkaTopic_;
            if (ref instanceof java.lang.String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                kafkaTopic_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        public static final int SUBSCRIPTION_FIELD_NUMBER = 8;
        private volatile java.lang.Object subscription_;
        /**
         * <code>string subscription = 8;</code>
         * @return The subscription.
         */
        @java.lang.Override
        public java.lang.String getSubscription() {
            java.lang.Object ref = subscription_;
            if (ref instanceof java.lang.String) {
                return (java.lang.String) ref;
            } else {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                subscription_ = s;
                return s;
            }
        }
        /**
         * <code>string subscription = 8;</code>
         * @return The bytes for subscription.
         */
        @java.lang.Override
        public com.google.protobuf.ByteString
        getSubscriptionBytes() {
            java.lang.Object ref = subscription_;
            if (ref instanceof java.lang.String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                subscription_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        public static final int PUBLISHING_TIMESTAMP_FIELD_NUMBER = 9;
        private long publishingTimestamp_;
        /**
         * <code>int64 publishing_timestamp = 9;</code>
         * @return The publishingTimestamp.
         */
        @java.lang.Override
        public long getPublishingTimestamp() {
            return publishingTimestamp_;
        }

        public static final int READING_TIMESTAMP_FIELD_NUMBER = 10;
        private long readingTimestamp_;
        /**
         * <code>int64 reading_timestamp = 10;</code>
         * @return The readingTimestamp.
         */
        @java.lang.Override
        public long getReadingTimestamp() {
            return readingTimestamp_;
        }

        public static final int BODY_FIELD_NUMBER = 11;
        private com.google.protobuf.ByteString body_;
        /**
         * <code>bytes body = 11;</code>
         * @return The body.
         */
        @java.lang.Override
        public com.google.protobuf.ByteString getBody() {
            return body_;
        }

        private byte memoizedIsInitialized = -1;
        @java.lang.Override
        public final boolean isInitialized() {
            byte isInitialized = memoizedIsInitialized;
            if (isInitialized == 1) return true;
            if (isInitialized == 0) return false;

            memoizedIsInitialized = 1;
            return true;
        }

        @java.lang.Override
        public void writeTo(com.google.protobuf.CodedOutputStream output)
                throws java.io.IOException {
            if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(messageId_)) {
                com.google.protobuf.GeneratedMessageV3.writeString(output, 1, messageId_);
            }
            if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(batchId_)) {
                com.google.protobuf.GeneratedMessageV3.writeString(output, 2, batchId_);
            }
            if (offset_ != 0L) {
                output.writeInt64(3, offset_);
            }
            if (partition_ != 0L) {
                output.writeInt64(4, partition_);
            }
            if (partitionAssignmentTerm_ != 0L) {
                output.writeInt64(5, partitionAssignmentTerm_);
            }
            if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(topic_)) {
                com.google.protobuf.GeneratedMessageV3.writeString(output, 6, topic_);
            }
            if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(kafkaTopic_)) {
                com.google.protobuf.GeneratedMessageV3.writeString(output, 7, kafkaTopic_);
            }
            if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(subscription_)) {
                com.google.protobuf.GeneratedMessageV3.writeString(output, 8, subscription_);
            }
            if (publishingTimestamp_ != 0L) {
                output.writeInt64(9, publishingTimestamp_);
            }
            if (readingTimestamp_ != 0L) {
                output.writeInt64(10, readingTimestamp_);
            }
            if (!body_.isEmpty()) {
                output.writeBytes(11, body_);
            }
            unknownFields.writeTo(output);
        }

        @java.lang.Override
        public int getSerializedSize() {
            int size = memoizedSize;
            if (size != -1) return size;

            size = 0;
            if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(messageId_)) {
                size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, messageId_);
            }
            if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(batchId_)) {
                size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, batchId_);
            }
            if (offset_ != 0L) {
                size += com.google.protobuf.CodedOutputStream
                        .computeInt64Size(3, offset_);
            }
            if (partition_ != 0L) {
                size += com.google.protobuf.CodedOutputStream
                        .computeInt64Size(4, partition_);
            }
            if (partitionAssignmentTerm_ != 0L) {
                size += com.google.protobuf.CodedOutputStream
                        .computeInt64Size(5, partitionAssignmentTerm_);
            }
            if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(topic_)) {
                size += com.google.protobuf.GeneratedMessageV3.computeStringSize(6, topic_);
            }
            if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(kafkaTopic_)) {
                size += com.google.protobuf.GeneratedMessageV3.computeStringSize(7, kafkaTopic_);
            }
            if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(subscription_)) {
                size += com.google.protobuf.GeneratedMessageV3.computeStringSize(8, subscription_);
            }
            if (publishingTimestamp_ != 0L) {
                size += com.google.protobuf.CodedOutputStream
                        .computeInt64Size(9, publishingTimestamp_);
            }
            if (readingTimestamp_ != 0L) {
                size += com.google.protobuf.CodedOutputStream
                        .computeInt64Size(10, readingTimestamp_);
            }
            if (!body_.isEmpty()) {
                size += com.google.protobuf.CodedOutputStream
                        .computeBytesSize(11, body_);
            }
            size += unknownFields.getSerializedSize();
            memoizedSize = size;
            return size;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord)) {
                return super.equals(obj);
            }
            pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord other = (pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord) obj;

            if (!getMessageId()
                    .equals(other.getMessageId())) return false;
            if (!getBatchId()
                    .equals(other.getBatchId())) return false;
            if (getOffset()
                    != other.getOffset()) return false;
            if (getPartition()
                    != other.getPartition()) return false;
            if (getPartitionAssignmentTerm()
                    != other.getPartitionAssignmentTerm()) return false;
            if (!getTopic()
                    .equals(other.getTopic())) return false;
            if (!getKafkaTopic()
                    .equals(other.getKafkaTopic())) return false;
            if (!getSubscription()
                    .equals(other.getSubscription())) return false;
            if (getPublishingTimestamp()
                    != other.getPublishingTimestamp()) return false;
            if (getReadingTimestamp()
                    != other.getReadingTimestamp()) return false;
            if (!getBody()
                    .equals(other.getBody())) return false;
            if (!unknownFields.equals(other.unknownFields)) return false;
            return true;
        }

        @java.lang.Override
        public int hashCode() {
            if (memoizedHashCode != 0) {
                return memoizedHashCode;
            }
            int hash = 41;
            hash = (19 * hash) + getDescriptor().hashCode();
            hash = (37 * hash) + MESSAGE_ID_FIELD_NUMBER;
            hash = (53 * hash) + getMessageId().hashCode();
            hash = (37 * hash) + BATCH_ID_FIELD_NUMBER;
            hash = (53 * hash) + getBatchId().hashCode();
            hash = (37 * hash) + OFFSET_FIELD_NUMBER;
            hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
                    getOffset());
            hash = (37 * hash) + PARTITION_FIELD_NUMBER;
            hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
                    getPartition());
            hash = (37 * hash) + PARTITION_ASSIGNMENT_TERM_FIELD_NUMBER;
            hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
                    getPartitionAssignmentTerm());
            hash = (37 * hash) + TOPIC_FIELD_NUMBER;
            hash = (53 * hash) + getTopic().hashCode();
            hash = (37 * hash) + KAFKA_TOPIC_FIELD_NUMBER;
            hash = (53 * hash) + getKafkaTopic().hashCode();
            hash = (37 * hash) + SUBSCRIPTION_FIELD_NUMBER;
            hash = (53 * hash) + getSubscription().hashCode();
            hash = (37 * hash) + PUBLISHING_TIMESTAMP_FIELD_NUMBER;
            hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
                    getPublishingTimestamp());
            hash = (37 * hash) + READING_TIMESTAMP_FIELD_NUMBER;
            hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
                    getReadingTimestamp());
            hash = (37 * hash) + BODY_FIELD_NUMBER;
            hash = (53 * hash) + getBody().hashCode();
            hash = (29 * hash) + unknownFields.hashCode();
            memoizedHashCode = hash;
            return hash;
        }

        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parseFrom(
                java.nio.ByteBuffer data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }
        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parseFrom(
                java.nio.ByteBuffer data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }
        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parseFrom(
                com.google.protobuf.ByteString data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }
        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parseFrom(
                com.google.protobuf.ByteString data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }
        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parseFrom(byte[] data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }
        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parseFrom(
                byte[] data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }
        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parseFrom(java.io.InputStream input)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseWithIOException(PARSER, input);
        }
        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parseFrom(
                java.io.InputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseWithIOException(PARSER, input, extensionRegistry);
        }
        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parseDelimitedFrom(java.io.InputStream input)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseDelimitedWithIOException(PARSER, input);
        }
        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parseDelimitedFrom(
                java.io.InputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
        }
        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parseFrom(
                com.google.protobuf.CodedInputStream input)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseWithIOException(PARSER, input);
        }
        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parseFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseWithIOException(PARSER, input, extensionRegistry);
        }

        @java.lang.Override
        public Builder newBuilderForType() { return newBuilder(); }
        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }
        public static Builder newBuilder(pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord prototype) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
        }
        @java.lang.Override
        public Builder toBuilder() {
            return this == DEFAULT_INSTANCE
                    ? new Builder() : new Builder().mergeFrom(this);
        }

        @java.lang.Override
        protected Builder newBuilderForType(
                com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
            Builder builder = new Builder(parent);
            return builder;
        }
        /**
         * Protobuf type {@code pl.allegro.tech.hermes.consumers.consumer.dead.Deadlettermessagerecord}
         */
        public static final class Builder extends
                com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
                // @@protoc_insertion_point(builder_implements:pl.allegro.tech.hermes.consumers.consumer.dead.Deadlettermessagerecord)
                pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.DeadlettermessagerecordOrBuilder {
            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.internal_static_pl_allegro_tech_hermes_consumers_consumer_dead_Deadlettermessagerecord_descriptor;
            }

            @java.lang.Override
            protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.internal_static_pl_allegro_tech_hermes_consumers_consumer_dead_Deadlettermessagerecord_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord.class, pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord.Builder.class);
            }

            // Construct using pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord.newBuilder()
            private Builder() {
                maybeForceBuilderInitialization();
            }

            private Builder(
                    com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                super(parent);
                maybeForceBuilderInitialization();
            }
            private void maybeForceBuilderInitialization() {
                if (com.google.protobuf.GeneratedMessageV3
                        .alwaysUseFieldBuilders) {
                }
            }
            @java.lang.Override
            public Builder clear() {
                super.clear();
                messageId_ = "";

                batchId_ = "";

                offset_ = 0L;

                partition_ = 0L;

                partitionAssignmentTerm_ = 0L;

                topic_ = "";

                kafkaTopic_ = "";

                subscription_ = "";

                publishingTimestamp_ = 0L;

                readingTimestamp_ = 0L;

                body_ = com.google.protobuf.ByteString.EMPTY;

                return this;
            }

            @java.lang.Override
            public com.google.protobuf.Descriptors.Descriptor
            getDescriptorForType() {
                return pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.internal_static_pl_allegro_tech_hermes_consumers_consumer_dead_Deadlettermessagerecord_descriptor;
            }

            @java.lang.Override
            public pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord getDefaultInstanceForType() {
                return pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord.getDefaultInstance();
            }

            @java.lang.Override
            public pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord build() {
                pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord result = buildPartial();
                if (!result.isInitialized()) {
                    throw newUninitializedMessageException(result);
                }
                return result;
            }

            @java.lang.Override
            public pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord buildPartial() {
                pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord result = new pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord(this);
                result.messageId_ = messageId_;
                result.batchId_ = batchId_;
                result.offset_ = offset_;
                result.partition_ = partition_;
                result.partitionAssignmentTerm_ = partitionAssignmentTerm_;
                result.topic_ = topic_;
                result.kafkaTopic_ = kafkaTopic_;
                result.subscription_ = subscription_;
                result.publishingTimestamp_ = publishingTimestamp_;
                result.readingTimestamp_ = readingTimestamp_;
                result.body_ = body_;
                onBuilt();
                return result;
            }

            @java.lang.Override
            public Builder clone() {
                return super.clone();
            }
            @java.lang.Override
            public Builder setField(
                    com.google.protobuf.Descriptors.FieldDescriptor field,
                    java.lang.Object value) {
                return super.setField(field, value);
            }
            @java.lang.Override
            public Builder clearField(
                    com.google.protobuf.Descriptors.FieldDescriptor field) {
                return super.clearField(field);
            }
            @java.lang.Override
            public Builder clearOneof(
                    com.google.protobuf.Descriptors.OneofDescriptor oneof) {
                return super.clearOneof(oneof);
            }
            @java.lang.Override
            public Builder setRepeatedField(
                    com.google.protobuf.Descriptors.FieldDescriptor field,
                    int index, java.lang.Object value) {
                return super.setRepeatedField(field, index, value);
            }
            @java.lang.Override
            public Builder addRepeatedField(
                    com.google.protobuf.Descriptors.FieldDescriptor field,
                    java.lang.Object value) {
                return super.addRepeatedField(field, value);
            }
            @java.lang.Override
            public Builder mergeFrom(com.google.protobuf.Message other) {
                if (other instanceof pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord) {
                    return mergeFrom((pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord)other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord other) {
                if (other == pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord.getDefaultInstance()) return this;
                if (!other.getMessageId().isEmpty()) {
                    messageId_ = other.messageId_;
                    onChanged();
                }
                if (!other.getBatchId().isEmpty()) {
                    batchId_ = other.batchId_;
                    onChanged();
                }
                if (other.getOffset() != 0L) {
                    setOffset(other.getOffset());
                }
                if (other.getPartition() != 0L) {
                    setPartition(other.getPartition());
                }
                if (other.getPartitionAssignmentTerm() != 0L) {
                    setPartitionAssignmentTerm(other.getPartitionAssignmentTerm());
                }
                if (!other.getTopic().isEmpty()) {
                    topic_ = other.topic_;
                    onChanged();
                }
                if (!other.getKafkaTopic().isEmpty()) {
                    kafkaTopic_ = other.kafkaTopic_;
                    onChanged();
                }
                if (!other.getSubscription().isEmpty()) {
                    subscription_ = other.subscription_;
                    onChanged();
                }
                if (other.getPublishingTimestamp() != 0L) {
                    setPublishingTimestamp(other.getPublishingTimestamp());
                }
                if (other.getReadingTimestamp() != 0L) {
                    setReadingTimestamp(other.getReadingTimestamp());
                }
                if (other.getBody() != com.google.protobuf.ByteString.EMPTY) {
                    setBody(other.getBody());
                }
                this.mergeUnknownFields(other.unknownFields);
                onChanged();
                return this;
            }

            @java.lang.Override
            public final boolean isInitialized() {
                return true;
            }

            @java.lang.Override
            public Builder mergeFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord parsedMessage = null;
                try {
                    parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    parsedMessage = (pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord) e.getUnfinishedMessage();
                    throw e.unwrapIOException();
                } finally {
                    if (parsedMessage != null) {
                        mergeFrom(parsedMessage);
                    }
                }
                return this;
            }

            private java.lang.Object messageId_ = "";
            /**
             * <code>string message_id = 1;</code>
             * @return The messageId.
             */
            public java.lang.String getMessageId() {
                java.lang.Object ref = messageId_;
                if (!(ref instanceof java.lang.String)) {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    messageId_ = s;
                    return s;
                } else {
                    return (java.lang.String) ref;
                }
            }
            /**
             * <code>string message_id = 1;</code>
             * @return The bytes for messageId.
             */
            public com.google.protobuf.ByteString
            getMessageIdBytes() {
                java.lang.Object ref = messageId_;
                if (ref instanceof String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    messageId_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }
            /**
             * <code>string message_id = 1;</code>
             * @param value The messageId to set.
             * @return This builder for chaining.
             */
            public Builder setMessageId(
                    java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }

                messageId_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>string message_id = 1;</code>
             * @return This builder for chaining.
             */
            public Builder clearMessageId() {

                messageId_ = getDefaultInstance().getMessageId();
                onChanged();
                return this;
            }
            /**
             * <code>string message_id = 1;</code>
             * @param value The bytes for messageId to set.
             * @return This builder for chaining.
             */
            public Builder setMessageIdBytes(
                    com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                checkByteStringIsUtf8(value);

                messageId_ = value;
                onChanged();
                return this;
            }

            private java.lang.Object batchId_ = "";
            /**
             * <code>string batch_id = 2;</code>
             * @return The batchId.
             */
            public java.lang.String getBatchId() {
                java.lang.Object ref = batchId_;
                if (!(ref instanceof java.lang.String)) {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    batchId_ = s;
                    return s;
                } else {
                    return (java.lang.String) ref;
                }
            }
            /**
             * <code>string batch_id = 2;</code>
             * @return The bytes for batchId.
             */
            public com.google.protobuf.ByteString
            getBatchIdBytes() {
                java.lang.Object ref = batchId_;
                if (ref instanceof String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    batchId_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }
            /**
             * <code>string batch_id = 2;</code>
             * @param value The batchId to set.
             * @return This builder for chaining.
             */
            public Builder setBatchId(
                    java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }

                batchId_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>string batch_id = 2;</code>
             * @return This builder for chaining.
             */
            public Builder clearBatchId() {

                batchId_ = getDefaultInstance().getBatchId();
                onChanged();
                return this;
            }
            /**
             * <code>string batch_id = 2;</code>
             * @param value The bytes for batchId to set.
             * @return This builder for chaining.
             */
            public Builder setBatchIdBytes(
                    com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                checkByteStringIsUtf8(value);

                batchId_ = value;
                onChanged();
                return this;
            }

            private long offset_ ;
            /**
             * <code>int64 offset = 3;</code>
             * @return The offset.
             */
            @java.lang.Override
            public long getOffset() {
                return offset_;
            }
            /**
             * <code>int64 offset = 3;</code>
             * @param value The offset to set.
             * @return This builder for chaining.
             */
            public Builder setOffset(long value) {

                offset_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>int64 offset = 3;</code>
             * @return This builder for chaining.
             */
            public Builder clearOffset() {

                offset_ = 0L;
                onChanged();
                return this;
            }

            private long partition_ ;
            /**
             * <code>int64 partition = 4;</code>
             * @return The partition.
             */
            @java.lang.Override
            public long getPartition() {
                return partition_;
            }
            /**
             * <code>int64 partition = 4;</code>
             * @param value The partition to set.
             * @return This builder for chaining.
             */
            public Builder setPartition(long value) {

                partition_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>int64 partition = 4;</code>
             * @return This builder for chaining.
             */
            public Builder clearPartition() {

                partition_ = 0L;
                onChanged();
                return this;
            }

            private long partitionAssignmentTerm_ ;
            /**
             * <code>int64 partition_assignment_term = 5;</code>
             * @return The partitionAssignmentTerm.
             */
            @java.lang.Override
            public long getPartitionAssignmentTerm() {
                return partitionAssignmentTerm_;
            }
            /**
             * <code>int64 partition_assignment_term = 5;</code>
             * @param value The partitionAssignmentTerm to set.
             * @return This builder for chaining.
             */
            public Builder setPartitionAssignmentTerm(long value) {

                partitionAssignmentTerm_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>int64 partition_assignment_term = 5;</code>
             * @return This builder for chaining.
             */
            public Builder clearPartitionAssignmentTerm() {

                partitionAssignmentTerm_ = 0L;
                onChanged();
                return this;
            }

            private java.lang.Object topic_ = "";
            /**
             * <code>string topic = 6;</code>
             * @return The topic.
             */
            public java.lang.String getTopic() {
                java.lang.Object ref = topic_;
                if (!(ref instanceof java.lang.String)) {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    topic_ = s;
                    return s;
                } else {
                    return (java.lang.String) ref;
                }
            }
            /**
             * <code>string topic = 6;</code>
             * @return The bytes for topic.
             */
            public com.google.protobuf.ByteString
            getTopicBytes() {
                java.lang.Object ref = topic_;
                if (ref instanceof String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    topic_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }
            /**
             * <code>string topic = 6;</code>
             * @param value The topic to set.
             * @return This builder for chaining.
             */
            public Builder setTopic(
                    java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }

                topic_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>string topic = 6;</code>
             * @return This builder for chaining.
             */
            public Builder clearTopic() {

                topic_ = getDefaultInstance().getTopic();
                onChanged();
                return this;
            }
            /**
             * <code>string topic = 6;</code>
             * @param value The bytes for topic to set.
             * @return This builder for chaining.
             */
            public Builder setTopicBytes(
                    com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                checkByteStringIsUtf8(value);

                topic_ = value;
                onChanged();
                return this;
            }

            private java.lang.Object kafkaTopic_ = "";
            /**
             * <code>string kafka_topic = 7;</code>
             * @return The kafkaTopic.
             */
            public java.lang.String getKafkaTopic() {
                java.lang.Object ref = kafkaTopic_;
                if (!(ref instanceof java.lang.String)) {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    kafkaTopic_ = s;
                    return s;
                } else {
                    return (java.lang.String) ref;
                }
            }
            /**
             * <code>string kafka_topic = 7;</code>
             * @return The bytes for kafkaTopic.
             */
            public com.google.protobuf.ByteString
            getKafkaTopicBytes() {
                java.lang.Object ref = kafkaTopic_;
                if (ref instanceof String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    kafkaTopic_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }
            /**
             * <code>string kafka_topic = 7;</code>
             * @param value The kafkaTopic to set.
             * @return This builder for chaining.
             */
            public Builder setKafkaTopic(
                    java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }

                kafkaTopic_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>string kafka_topic = 7;</code>
             * @return This builder for chaining.
             */
            public Builder clearKafkaTopic() {

                kafkaTopic_ = getDefaultInstance().getKafkaTopic();
                onChanged();
                return this;
            }
            /**
             * <code>string kafka_topic = 7;</code>
             * @param value The bytes for kafkaTopic to set.
             * @return This builder for chaining.
             */
            public Builder setKafkaTopicBytes(
                    com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                checkByteStringIsUtf8(value);

                kafkaTopic_ = value;
                onChanged();
                return this;
            }

            private java.lang.Object subscription_ = "";
            /**
             * <code>string subscription = 8;</code>
             * @return The subscription.
             */
            public java.lang.String getSubscription() {
                java.lang.Object ref = subscription_;
                if (!(ref instanceof java.lang.String)) {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    subscription_ = s;
                    return s;
                } else {
                    return (java.lang.String) ref;
                }
            }
            /**
             * <code>string subscription = 8;</code>
             * @return The bytes for subscription.
             */
            public com.google.protobuf.ByteString
            getSubscriptionBytes() {
                java.lang.Object ref = subscription_;
                if (ref instanceof String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    subscription_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }
            /**
             * <code>string subscription = 8;</code>
             * @param value The subscription to set.
             * @return This builder for chaining.
             */
            public Builder setSubscription(
                    java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }

                subscription_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>string subscription = 8;</code>
             * @return This builder for chaining.
             */
            public Builder clearSubscription() {

                subscription_ = getDefaultInstance().getSubscription();
                onChanged();
                return this;
            }
            /**
             * <code>string subscription = 8;</code>
             * @param value The bytes for subscription to set.
             * @return This builder for chaining.
             */
            public Builder setSubscriptionBytes(
                    com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                checkByteStringIsUtf8(value);

                subscription_ = value;
                onChanged();
                return this;
            }

            private long publishingTimestamp_ ;
            /**
             * <code>int64 publishing_timestamp = 9;</code>
             * @return The publishingTimestamp.
             */
            @java.lang.Override
            public long getPublishingTimestamp() {
                return publishingTimestamp_;
            }
            /**
             * <code>int64 publishing_timestamp = 9;</code>
             * @param value The publishingTimestamp to set.
             * @return This builder for chaining.
             */
            public Builder setPublishingTimestamp(long value) {

                publishingTimestamp_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>int64 publishing_timestamp = 9;</code>
             * @return This builder for chaining.
             */
            public Builder clearPublishingTimestamp() {

                publishingTimestamp_ = 0L;
                onChanged();
                return this;
            }

            private long readingTimestamp_ ;
            /**
             * <code>int64 reading_timestamp = 10;</code>
             * @return The readingTimestamp.
             */
            @java.lang.Override
            public long getReadingTimestamp() {
                return readingTimestamp_;
            }
            /**
             * <code>int64 reading_timestamp = 10;</code>
             * @param value The readingTimestamp to set.
             * @return This builder for chaining.
             */
            public Builder setReadingTimestamp(long value) {

                readingTimestamp_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>int64 reading_timestamp = 10;</code>
             * @return This builder for chaining.
             */
            public Builder clearReadingTimestamp() {

                readingTimestamp_ = 0L;
                onChanged();
                return this;
            }

            private com.google.protobuf.ByteString body_ = com.google.protobuf.ByteString.EMPTY;
            /**
             * <code>bytes body = 11;</code>
             * @return The body.
             */
            @java.lang.Override
            public com.google.protobuf.ByteString getBody() {
                return body_;
            }
            /**
             * <code>bytes body = 11;</code>
             * @param value The body to set.
             * @return This builder for chaining.
             */
            public Builder setBody(com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }

                body_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>bytes body = 11;</code>
             * @return This builder for chaining.
             */
            public Builder clearBody() {

                body_ = getDefaultInstance().getBody();
                onChanged();
                return this;
            }
            @java.lang.Override
            public final Builder setUnknownFields(
                    final com.google.protobuf.UnknownFieldSet unknownFields) {
                return super.setUnknownFields(unknownFields);
            }

            @java.lang.Override
            public final Builder mergeUnknownFields(
                    final com.google.protobuf.UnknownFieldSet unknownFields) {
                return super.mergeUnknownFields(unknownFields);
            }


            // @@protoc_insertion_point(builder_scope:pl.allegro.tech.hermes.consumers.consumer.dead.Deadlettermessagerecord)
        }

        // @@protoc_insertion_point(class_scope:pl.allegro.tech.hermes.consumers.consumer.dead.Deadlettermessagerecord)
        private static final pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord DEFAULT_INSTANCE;
        static {
            DEFAULT_INSTANCE = new pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord();
        }

        public static pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        private static final com.google.protobuf.Parser<Deadlettermessagerecord>
                PARSER = new com.google.protobuf.AbstractParser<Deadlettermessagerecord>() {
            @java.lang.Override
            public Deadlettermessagerecord parsePartialFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return new Deadlettermessagerecord(input, extensionRegistry);
            }
        };

        public static com.google.protobuf.Parser<Deadlettermessagerecord> parser() {
            return PARSER;
        }

        @java.lang.Override
        public com.google.protobuf.Parser<Deadlettermessagerecord> getParserForType() {
            return PARSER;
        }

        @java.lang.Override
        public pl.allegro.tech.hermes.consumers.consumer.dead.DeadLetterMessageProtoRecord.Deadlettermessagerecord getDefaultInstanceForType() {
            return DEFAULT_INSTANCE;
        }

    }

    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_pl_allegro_tech_hermes_consumers_consumer_dead_Deadlettermessagerecord_descriptor;
    private static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_pl_allegro_tech_hermes_consumers_consumer_dead_Deadlettermessagerecord_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor
    getDescriptor() {
        return descriptor;
    }
    private static  com.google.protobuf.Descriptors.FileDescriptor
            descriptor;
    static {
        java.lang.String[] descriptorData = {
                "\n\"DeadLetterMessageProtoRecord.proto\022.pl" +
                        ".allegro.tech.hermes.consumers.consumer." +
                        "dead\"\206\002\n\027Deadlettermessagerecord\022\022\n\nmess" +
                        "age_id\030\001 \001(\t\022\020\n\010batch_id\030\002 \001(\t\022\016\n\006offset" +
                        "\030\003 \001(\003\022\021\n\tpartition\030\004 \001(\003\022!\n\031partition_a" +
                        "ssignment_term\030\005 \001(\003\022\r\n\005topic\030\006 \001(\t\022\023\n\013k" +
                        "afka_topic\030\007 \001(\t\022\024\n\014subscription\030\010 \001(\t\022\034" +
                        "\n\024publishing_timestamp\030\t \001(\003\022\031\n\021reading_" +
                        "timestamp\030\n \001(\003\022\014\n\004body\030\013 \001(\014b\006proto3"
        };
        descriptor = com.google.protobuf.Descriptors.FileDescriptor
                .internalBuildGeneratedFileFrom(descriptorData,
                        new com.google.protobuf.Descriptors.FileDescriptor[] {
                        });
        internal_static_pl_allegro_tech_hermes_consumers_consumer_dead_Deadlettermessagerecord_descriptor =
                getDescriptor().getMessageTypes().get(0);
        internal_static_pl_allegro_tech_hermes_consumers_consumer_dead_Deadlettermessagerecord_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                internal_static_pl_allegro_tech_hermes_consumers_consumer_dead_Deadlettermessagerecord_descriptor,
                new java.lang.String[] { "MessageId", "BatchId", "Offset", "Partition", "PartitionAssignmentTerm", "Topic", "KafkaTopic", "Subscription", "PublishingTimestamp", "ReadingTimestamp", "Body", });
    }

    // @@protoc_insertion_point(outer_class_scope)
}
