package pl.allegro.tech.hermes.consumers.consumer.dead;

import com.google.cloud.ByteArray;
import com.google.cloud.bigquery.storage.v1.TableSchema;
import com.google.cloud.bigquery.storage.v1.ToProtoConverter;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import pl.allegro.tech.hermes.tracker.consumers.deadletters.DeadMessage;

import java.util.Dictionary;
import java.util.Hashtable;

public class DeadMessageToProtoConverter implements ToProtoConverter<DeadMessage> {
    @Override
    public DynamicMessage convertToProtoMessage(Descriptors.Descriptor descriptor, TableSchema tableSchema, DeadMessage deadMessage, boolean b) {
        DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(descriptor);

        Dictionary<String, Object> dictionary = convertDeadMessageToDictionary(deadMessage);
        for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
            messageBuilder.setField(field, dictionary.get(field.getName()));
        }
        return messageBuilder.build();

    }

    private Dictionary<String, Object> convertDeadMessageToDictionary(DeadMessage deadMessage) {
        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put("messageid", deadMessage.getMessageId());
        dictionary.put("batchid", deadMessage.getBatchId());
        dictionary.put("offset", deadMessage.getOffset());
        dictionary.put("partition", ((Number)deadMessage.getPartition()).longValue());
        dictionary.put("partitionassignmentterm", deadMessage.getPartitionAssignmentTerm());
        dictionary.put("topic", deadMessage.getTopic());
        dictionary.put("subscription", deadMessage.getSubscription());
        dictionary.put("kafkatopic", deadMessage.getKafkaTopic());
        dictionary.put("publishingtimestamp", deadMessage.getPublishingTimestamp());
        dictionary.put("readingtimestamp", deadMessage.getReadingTimestamp());
        dictionary.put("body", ByteString.copyFrom(deadMessage.getBody()));
        return dictionary;
    }
}
