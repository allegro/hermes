import type { InconsistentKafkaTopic } from '@/api/kafka-inconsistency';

export const dummyKafkaClusters = ['dc', 'gcp'];

export const dummyKafkaConfigInconsistencies: InconsistentKafkaTopic[] = [
  {
    qualifiedTopicName: 'pl.allegro.public.offer.OfferEventV1',
    kafkaTopicName: 'pl.allegro.public.offer.OfferEventV1_avro',
    clusterName: 'gcp',
    existsOnBroker: true,
    configDiffs: [
      {
        key: 'retention.ms',
        expected: '86400000',
        actual: null,
      },
    ],
  },
  {
    qualifiedTopicName: 'pl.allegro.public.order.OrderEventV1',
    kafkaTopicName: 'pl.allegro.public.order.OrderEventV1_avro',
    clusterName: 'gcp',
    existsOnBroker: false,
    configDiffs: [],
  },
];
