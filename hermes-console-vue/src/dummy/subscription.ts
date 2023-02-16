import { ContentType } from '@/api/content-type';
import {
  DeliveryType,
  Severity,
  State,
  SubscriptionMode,
} from '@/api/subscription';
import { ProblemCode, Status } from '@/api/subscription-health';
import { SentMessageTraceStatus } from '@/api/subscription-undelivered';
import type { SentMessageTrace } from '@/api/subscription-undelivered';
import type { Subscription } from '@/api/subscription';
import type { SubscriptionHealth } from '@/api/subscription-health';
import type { SubscriptionMetrics } from '@/api/subscription-metrics';

export const dummySubscription: Subscription = {
  topicName: 'pl.allegro.public.group.DummyEvent',
  name: 'foobar-service',
  endpoint: 'service://foobar-service/events/dummy-event',
  state: State.ACTIVE,
  description: 'Test Hermes endpoint',
  subscriptionPolicy: {
    rate: 10,
    messageTtl: 60,
    messageBackoff: 100,
    requestTimeout: 1000,
    socketTimeout: 0,
    sendingDelay: 0,
    backoffMultiplier: 1.0,
    backoffMaxIntervalInSec: 600,
    retryClientErrors: true,
    backoffMaxIntervalMillis: 600000,
  },
  trackingEnabled: false,
  trackingMode: 'trackingOff',
  owner: {
    source: 'Service Catalog',
    id: '42',
  },
  monitoringDetails: {
    severity: Severity.NON_IMPORTANT,
    reaction: '',
  },
  contentType: ContentType.JSON,
  deliveryType: DeliveryType.SERIAL,
  filters: [
    {
      type: 'avropath',
      path: 'foobar',
      matcher: '^FOO_BAR$|^BAZ_BAR$',
      matchingStrategy: 'any',
    },
  ],
  mode: SubscriptionMode.ANYCAST,
  headers: [],
  endpointAddressResolverMetadata: {},
  http2Enabled: false,
  subscriptionIdentityHeadersEnabled: false,
  autoDeleteWithTopicEnabled: false,
  createdAt: 1579507131.238,
  modifiedAt: 1672140855.813,
};

export const dummySubscriptionMetrics: SubscriptionMetrics = {
  delivered: 39099,
  discarded: 2137086,
  volume: 1288032256,
  timeouts: '0.0',
  otherErrors: '0.0',
  codes2xx: '0',
  codes4xx: '0.0',
  codes5xx: '0.01',
  rate: '0',
  throughput: '8.31',
  batchRate: '0.0',
  lag: '9055513',
};

export const dummySubscriptionHealth: SubscriptionHealth = {
  status: Status.HEALTHY,
  problems: [
    {
      code: ProblemCode.LAGGING,
      description: '',
    },
  ],
};

export const dummyUndeliveredMessage: SentMessageTrace = {
  timestamp: 1234567890,
  subscription: 'foobar-service',
  topicName: 'pl.allegro.public.group.DummyEvent',
  status: SentMessageTraceStatus.DISCARDED,
  reason: 'Message sending failed with status code: 500',
  message: 'some message',
  partition: 7,
  offset: 217294378,
  cluster: 'kafka-cluster',
};

export const dummyUndeliveredMessages: SentMessageTrace[] = [
  dummyUndeliveredMessage,
];
