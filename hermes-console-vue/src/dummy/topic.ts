import type {
  MessagePreview,
  TopicMetrics,
  TopicWithSchema,
} from '@/api/topic';
import { ContentType } from '@/api/content-type';
import type { Owner } from '@/api/owner';
import { Ack } from '@/api/topic';

export const dummyTopic: TopicWithSchema = {
  schema:
    '{"type":"record","name":"DummyEvent","namespace":"pl.allegro.public.group.DummyEvent","doc":"Event emitted when notification is sent to a user","fields":[{"name":"__metadata","type":["null",{"type":"map","values":"string"}],"doc":"Field internally used by Hermes to propagate metadata.","default":null},{"name":"waybillId","type":["null",{"type":"record","name":"WaybillId","fields":[{"name":"waybill","type":"string","doc":"Waybill"},{"name":"carrierId","type":"string","doc":"CarrierId"}]}],"doc":"WaybillId","default":null},{"name":"notificationId","type":"string","doc":"Notification Id"},{"name":"userId","type":"string","doc":"User Id"}]}',
  name: 'pl.allegro.public.group.DummyEvent',
  description: 'Events emitted when notification is sent to a user.',
  owner: {
    source: 'Service Catalog',
    id: '41',
  },
  retentionTime: {
    duration: 1,
    retentionUnit: 'DAYS',
  },
  jsonToAvroDryRun: false,
  ack: Ack.LEADER,
  trackingEnabled: false,
  migratedFromJsonType: false,
  schemaIdAwareSerializationEnabled: false,
  contentType: ContentType.AVRO,
  maxMessageSize: 10240,
  auth: {
    publishers: [],
    enabled: false,
    unauthenticatedAccessEnabled: true,
  },
  subscribingRestricted: false,
  offlineStorage: {
    enabled: true,
    retentionTime: {
      duration: 60,
      infinite: false,
    },
  },
  labels: [
    {
      value: 'internal',
    },
    {
      value: 'analytics',
    },
  ],
  createdAt: 1634916242.877,
  modifiedAt: 1636451113.517,
};

export const dummyTopicOwner: Owner = {
  id: '41',
  name: 'your-super-service',
  url: 'https://google.pl?q=your-super-service',
};

export const dummyTopicMessagesPreview: MessagePreview[] = [
  {
    content:
      '{"__metadata":{"x-request-id":"65157233-0153-4256-91d6-12d5b60d47a2","messageId":"32fdedf7-a425-4sad-ad85-dd3fec785ccd","trace-sampled":"0","timestamp":"1652257893073"},"waybillId":{"waybill":"1234567890000","carrierId":"SAMPLE"},"notificationId":"142a3f4a-a56e-789c-b866-2a27b0d24cb1","userId":"12345678"}',
    truncated: false,
  },
];

export const dummyTopicMetrics: TopicMetrics = {
  published: 100,
  volume: 200,
  rate: '3.4',
  deliveryRate: '3.5',
  subscriptions: 2,
  throughput: '3.6',
};
