// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: schemas/primitives/BoolProto.proto

package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor;

public final class BoolProto {
  private BoolProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface PrimitivesBoolOrBuilder extends
      // @@protoc_insertion_point(interface_extends:pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.PrimitivesBool)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>bool field = 1;</code>
     * @return The field.
     */
    boolean getField();
  }
  /**
   * Protobuf type {@code pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.PrimitivesBool}
   */
  public static final class PrimitivesBool extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.PrimitivesBool)
      PrimitivesBoolOrBuilder {
  private static final long serialVersionUID = 0L;
    // Use PrimitivesBool.newBuilder() to construct.
    private PrimitivesBool(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private PrimitivesBool() {
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
        UnusedPrivateParameter unused) {
      return new PrimitivesBool();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }
    private PrimitivesBool(
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
            case 8: {

              field_ = input.readBool();
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
      return pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.internal_static_pl_allegro_tech_hermes_consumers_consumer_sender_googlebigquery_avro_descriptor_PrimitivesBool_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.internal_static_pl_allegro_tech_hermes_consumers_consumer_sender_googlebigquery_avro_descriptor_PrimitivesBool_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool.class, pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool.Builder.class);
    }

    public static final int FIELD_FIELD_NUMBER = 1;
    private boolean field_;
    /**
     * <code>bool field = 1;</code>
     * @return The field.
     */
    @java.lang.Override
    public boolean getField() {
      return field_;
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
      if (field_ != false) {
        output.writeBool(1, field_);
      }
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (field_ != false) {
        size += com.google.protobuf.CodedOutputStream
          .computeBoolSize(1, field_);
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
      if (!(obj instanceof pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool)) {
        return super.equals(obj);
      }
      pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool other = (pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool) obj;

      if (getField()
          != other.getField()) return false;
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
      hash = (37 * hash) + FIELD_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(
          getField());
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parseFrom(
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
    public static Builder newBuilder(pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool prototype) {
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
     * Protobuf type {@code pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.PrimitivesBool}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.PrimitivesBool)
        pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBoolOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.internal_static_pl_allegro_tech_hermes_consumers_consumer_sender_googlebigquery_avro_descriptor_PrimitivesBool_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.internal_static_pl_allegro_tech_hermes_consumers_consumer_sender_googlebigquery_avro_descriptor_PrimitivesBool_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool.class, pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool.Builder.class);
      }

      // Construct using pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool.newBuilder()
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
        field_ = false;

        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.internal_static_pl_allegro_tech_hermes_consumers_consumer_sender_googlebigquery_avro_descriptor_PrimitivesBool_descriptor;
      }

      @java.lang.Override
      public pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool getDefaultInstanceForType() {
        return pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool.getDefaultInstance();
      }

      @java.lang.Override
      public pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool build() {
        pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool buildPartial() {
        pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool result = new pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool(this);
        result.field_ = field_;
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
        if (other instanceof pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool) {
          return mergeFrom((pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool other) {
        if (other == pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool.getDefaultInstance()) return this;
        if (other.getField() != false) {
          setField(other.getField());
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
        pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private boolean field_ ;
      /**
       * <code>bool field = 1;</code>
       * @return The field.
       */
      @java.lang.Override
      public boolean getField() {
        return field_;
      }
      /**
       * <code>bool field = 1;</code>
       * @param value The field to set.
       * @return This builder for chaining.
       */
      public Builder setField(boolean value) {
        
        field_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>bool field = 1;</code>
       * @return This builder for chaining.
       */
      public Builder clearField() {
        
        field_ = false;
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


      // @@protoc_insertion_point(builder_scope:pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.PrimitivesBool)
    }

    // @@protoc_insertion_point(class_scope:pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.PrimitivesBool)
    private static final pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool();
    }

    public static pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<PrimitivesBool>
        PARSER = new com.google.protobuf.AbstractParser<PrimitivesBool>() {
      @java.lang.Override
      public PrimitivesBool parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new PrimitivesBool(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<PrimitivesBool> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<PrimitivesBool> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.descriptor.BoolProto.PrimitivesBool getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_pl_allegro_tech_hermes_consumers_consumer_sender_googlebigquery_avro_descriptor_PrimitivesBool_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_pl_allegro_tech_hermes_consumers_consumer_sender_googlebigquery_avro_descriptor_PrimitivesBool_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\"schemas/primitives/BoolProto.proto\022Opl" +
      ".allegro.tech.hermes.consumers.consumer." +
      "sender.googlebigquery.avro.descriptor\"\037\n" +
      "\016PrimitivesBool\022\r\n\005field\030\001 \001(\010b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_pl_allegro_tech_hermes_consumers_consumer_sender_googlebigquery_avro_descriptor_PrimitivesBool_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_pl_allegro_tech_hermes_consumers_consumer_sender_googlebigquery_avro_descriptor_PrimitivesBool_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_pl_allegro_tech_hermes_consumers_consumer_sender_googlebigquery_avro_descriptor_PrimitivesBool_descriptor,
        new java.lang.String[] { "Field", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
