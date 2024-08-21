import type { InconsistentGroup } from '@/api/inconsistent-group';

export const dummyGroupInconsistency: InconsistentGroup[] = [
  {
    name: 'pl.allegro.public.group',
    inconsistentMetadata: [],
    inconsistentTopics: [
      {
        name: 'pl.allegro.public.group.DummyEvent',
        inconsistentMetadata: [],
        inconsistentSubscriptions: [
          {
            name: 'pl.allegro.public.group.DummyEvent$foobar-service',
            inconsistentMetadata: [
              {
                datacenter: 'DC1',
              },
              {
                datacenter: 'DC2',
                content:
                  '{\n  "id": "foobar-service",\n  "topicName": "pl.allegro.public.group.DummyEvent",\n  "name": "foobar-service",\n  "endpoint": "service://foobar-service/events/dummy-event",\n  "state": "ACTIVE",\n  "description": "Test Hermes endpoint",\n  "subscriptionPolicy": {\n    "rate": 10,\n    "messageTtl": 60,\n    "messageBackoff": 100,\n    "requestTimeout": 1000,\n    "socketTimeout": 0,\n    "sendingDelay": 0,\n    "backoffMultiplier": 1.0,\n    "backoffMaxIntervalInSec": 600,\n    "retryClientErrors": true,\n    "backoffMaxIntervalMillis": 600000\n  },\n  "trackingEnabled": false,\n  "trackingMode": "trackingOff",\n  "owner": {\n    "source": "Service Catalog",\n    "id": "42"\n  },\n  "monitoringDetails": {\n    "severity": "NON_IMPORTANT",\n    "reaction": ""\n  },\n  "contentType": "JSON",\n  "deliveryType": "SERIAL",\n  "filters": [\n    {\n      "type": "avropath",\n      "path": "foobar",\n      "matcher": "^FOO_BAR$|^BAZ_BAR$",\n      "matchingStrategy": "any"\n    },\n    {\n      "type": "avropath",\n      "path": ".foo.bar.baz",\n      "matcher": "true",\n      "matchingStrategy": "all"\n    }\n  ],\n  "mode": "ANYCAST",\n  "headers": [\n    {\n      "name": "X-My-Header",\n      "value": "boobar"\n    },\n    {\n      "name": "X-Another-Header",\n      "value": "foobar"\n    }\n  ],\n  "endpointAddressResolverMetadata": {\n    "additionalMetadata": false,\n    "nonSupportedProperty": 2\n  },\n  "http2Enabled": false,\n  "subscriptionIdentityHeadersEnabled": false,\n  "autoDeleteWithTopicEnabled": false,\n  "createdAt": 1579507131.238,\n  "modifiedAt": 1672140855.813\n}',
              },
            ],
          },
        ],
      },
    ],
  },
];

export const dummyGroupInconsistency2: InconsistentGroup[] = [
  {
    name: 'pl.allegro.public.group',
    inconsistentMetadata: [],
    inconsistentTopics: [
      {
        name: 'pl.allegro.public.group.DummyEvent',
        inconsistentMetadata: [],
        inconsistentSubscriptions: [],
      },
    ],
  },
];

export const dummyGroupInconsistency3: InconsistentGroup[] = [
  {
    name: 'pl.allegro.public.group',
    inconsistentMetadata: [
      {
        datacenter: 'DC1',
        content: '{"lorem": "ipsum"}',
      },
      {
        datacenter: 'DC2',
        content: '{"lorem": "ipsum"}',
      },
    ],
    inconsistentTopics: [],
  },
];

export const dummyGroupInconsistency4: InconsistentGroup[] = [
  {
    name: 'pl.allegro.public.group',
    inconsistentMetadata: [
      {
        datacenter: 'DC1',
        content: '{"lorem": "ipsum"}',
      },
      {
        datacenter: 'DC2',
        content: '{"lorem": "ipsum"}',
      },
    ],
    inconsistentTopics: [],
  },
  {
    name: 'pl.allegro.public.group2',
    inconsistentMetadata: [
      {
        datacenter: 'DC1',
        content: '{"lorem": "ipsum"}',
      },
      {
        datacenter: 'DC2',
        content: '{"lorem": "ipsum"}',
      },
    ],
    inconsistentTopics: [],
  },
];
