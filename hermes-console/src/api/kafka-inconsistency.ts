export interface KafkaTopicConfigDiff {
  key: string;
  expected: string | null;
  actual: string | null;
}

export interface InconsistentKafkaTopic {
  qualifiedTopicName: string;
  kafkaTopicName: string;
  clusterName: string;
  existsOnBroker: boolean;
  configDiffs: KafkaTopicConfigDiff[];
}

export function topicKey(topic: InconsistentKafkaTopic) {
  return `${topic.clusterName}:${topic.kafkaTopicName}`;
}
