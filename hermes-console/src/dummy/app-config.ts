import type { AppConfiguration } from '@/api/app-configuration';

export const dummyAppConfig: AppConfiguration = {
  console: {
    title: 'hermes console',
    contactLink: '',
    environmentName: 'local',
    criticalEnvironment: false,
  },
  dashboard: {
    metrics: '',
    docs: 'http://hermes-pubsub.rtfd.org',
  },
  metrics: {
    fetchingDashboardUrlEnabled: true,
  },
  auth: {
    oauth: {
      enabled: false,
      url: 'http://localhost:8080',
      authorizationEndpoint: '/authorization',
      tokenEndpoint: '/token',
      clientId: 'hermes',
      scope: '',
    },
  },
  owner: {
    sources: [
      {
        name: 'Service Catalog',
        placeholder: 'service name from Service Catalog',
      },
    ],
  },
  topic: {
    messagePreviewEnabled: true,
    offlineClientsEnabled: true,
    defaults: {
      ack: 'LEADER',
      contentType: 'AVRO',
      retentionTime: {
        duration: 1,
        retentionUnit: 'DAYS',
      },
      offlineStorage: {
        enabled: false,
        retentionTime: {
          duration: 1,
          retentionUnit: 'DAYS',
        },
      },
    },
    contentTypes: [
      {
        value: 'AVRO',
        label: 'AVRO',
      },
    ],
    readOnlyModeEnabled: false,
    retentionUnits: [
      {
        value: 'DAYS',
        label: 'DAYS',
      },
      {
        value: 'HOURS',
        label: 'HOURS',
      },
    ],
    offlineRetransmission: {
      enabled: true,
      description:
        'Offline retransmission allows retransmitting events from GCP (BigQuery) to Hermes.',
      fromViewDocsUrl: 'https://hermes-pubsub.rtfd.org',
      globalTaskQueueUrl: 'http://localhost:8090/offline-retransmission/tasks',
      monitoringDocsUrl: 'https://hermes-pubsub.rtfd.org',
    },
  },
  subscription: {
    endpointAddressResolverMetadata: {
      supportedMetadata: {
        title: 'Supported metadata',
        type: 'boolean',
        hint: 'Some hint',
      },
    },
    requestTimeoutWarningThreshold: 1001,
    defaults: {
      subscriptionPolicy: {
        messageTtl: 60,
        requestTimeout: 1000,
      },
      deliveryType: 'SERIAL',
    },
    deliveryTypes: [
      {
        value: 'SERIAL',
        label: 'SERIAL',
      },
      {
        value: 'BATCH',
        label: 'BATCH',
      },
    ],
  },
  consistency: {
    maxGroupBatchSize: 10,
  },
  group: {
    nonAdminCreationEnabled: true,
  },
  costs: {
    enabled: true,
    globalDetailsUrl: '',
    topicIframeUrl: '',
    topicDetailsUrl: '',
    subscriptionIframeUrl: '',
    subscriptionDetailsUrl: '',
  },
};
