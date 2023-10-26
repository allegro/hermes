import { ConsumerGroupState } from '@/api/consumer-group';
import { ContentType } from '@/api/content-type';
import type { ConsumerGroup } from '@/api/consumer-group';

export const dummyConsumerGroups: ConsumerGroup[] = [
  {
    clusterName: 'kafka-1',
    groupId: 'pl.allegro.public.group_DummyEvent_foobar-service',
    state: ConsumerGroupState.STABLE,
    members: [
      {
        clientId: 'pl-allegro-tech-hermes.hermes-consumers-1',
        consumerId: 'pl-allegro-tech-hermes.hermes-consumers-1_consumer_1',
        host: '123.11.22.33',
        partitions: [
          {
            partition: 1,
            contentType: ContentType.AVRO,
            currentOffset: 2017,
            lag: 3,
            logEndOffset: 2020,
            offsetMetadata: '',
            topic: 'pl.allegro.public.group.DummyEvent_avro',
          },
          {
            partition: 2,
            contentType: ContentType.JSON,
            currentOffset: 2017,
            lag: 0,
            logEndOffset: 2017,
            offsetMetadata: '',
            topic: 'pl.allegro.public.group.DummyEvent_json',
          },
        ],
      },
    ],
  },
  {
    clusterName: 'kafka-2',
    groupId: 'pl.allegro.public.group_DummyEvent_foobar-service',
    state: ConsumerGroupState.DEAD,
    members: [
      {
        clientId: 'pl-allegro-tech-hermes.hermes-consumers-2',
        consumerId: 'pl-allegro-tech-hermes.hermes-consumers-2_consumer_1',
        host: '123.11.22.34',
        partitions: [
          {
            partition: 1,
            contentType: ContentType.JSON,
            currentOffset: 2017,
            lag: 3,
            logEndOffset: 2020,
            offsetMetadata: '',
            topic: 'pl.allegro.public.group.DummyEvent_avro',
          },
          {
            partition: 2,
            contentType: ContentType.AVRO,
            currentOffset: 2017,
            lag: 0,
            logEndOffset: 2017,
            offsetMetadata: '',
            topic: 'pl.allegro.public.group.DummyEvent_json',
          },
        ],
      },
      {
        clientId: 'pl-allegro-tech-hermes.hermes-consumers-3',
        consumerId: 'pl-allegro-tech-hermes.hermes-consumers-3_consumer_1',
        host: '123.11.22.35',
        partitions: [
          {
            partition: 1,
            contentType: ContentType.AVRO,
            currentOffset: 2017123,
            lag: 312333,
            logEndOffset: 2020123,
            offsetMetadata: '',
            topic: 'pl.allegro.public.group.DummyEvent_avro',
          },
          {
            partition: 2,
            contentType: ContentType.JSON,
            currentOffset: 2017,
            lag: 0,
            logEndOffset: 2017,
            offsetMetadata: '',
            topic: 'pl.allegro.public.group.DummyEvent_json',
          },
        ],
      },
    ],
  },
];
