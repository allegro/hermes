import {
  dummySubscription,
  dummyTopicSubscriptionsList,
  secondDummySubscription,
} from '@/dummy/subscription';
import {
  dummyTopic,
  dummyTopicMessagesPreview,
  dummyTopicMetrics,
  dummyTopicOwner,
} from '@/dummy/topic';
import { rest } from 'msw';
import type { AccessTokenResponse } from '@/api/access-token-response';
import type { ConstraintsConfig } from '@/api/constraints';
import type { ConsumerGroup } from '@/api/consumer-group';
import type { DatacenterReadiness } from '@/api/datacenter-readiness';
import type {
  MessagePreview,
  TopicMetrics,
  TopicWithSchema,
} from '@/api/topic';
import type { Owner } from '@/api/owner';
import type { Roles } from '@/api/roles';
import type { Stats } from '@/api/stats';
import type { Subscription } from '@/api/subscription';

const url = 'http://localhost:3000';

export const fetchTopicHandler = ({
  topic = dummyTopic,
}: {
  topic?: TopicWithSchema;
}) =>
  rest.get(`${url}/topics/${topic.name}`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(topic));
  });

export const fetchTopicErrorHandler = ({
  topicName,
  errorCode = 500,
}: {
  topicName: string;
  errorCode?: number;
}) =>
  rest.get(`${url}/topics/${topicName}`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchTopicOwnerHandler = ({
  topicOwner = dummyTopicOwner,
}: {
  topicOwner?: Owner;
}) =>
  rest.get(
    `${url}/owners/sources/Service%20Catalog/${topicOwner.id}`,
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(topicOwner));
    },
  );

export const fetchTopicOwnerErrorHandler = ({
  topicOwner,
  errorCode = 500,
}: {
  topicOwner: string;
  errorCode?: number;
}) =>
  rest.get(
    `${url}/owners/sources/Service%20Catalog/${topicOwner}`,
    (req, res, ctx) => {
      return res(ctx.status(errorCode), ctx.json(undefined));
    },
  );

export const fetchTopicMessagesPreviewHandler = ({
  topicName,
  messages = dummyTopicMessagesPreview,
}: {
  topicName: string;
  messages?: MessagePreview[];
}) =>
  rest.get(`${url}/topics/${topicName}/preview`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(messages));
  });

export const fetchTopicMessagesPreviewErrorHandler = ({
  topicName,
  errorCode = 500,
}: {
  topicName: string;
  errorCode?: number;
}) =>
  rest.get(`${url}/topics/${topicName}/preview`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchTopicMetricsHandler = ({
  topicName,
  metrics = dummyTopicMetrics,
}: {
  topicName: string;
  metrics?: TopicMetrics;
}) =>
  rest.get(`${url}/topics/${topicName}/metrics`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(metrics));
  });

export const fetchTopicMetricsErrorHandler = ({
  topicName,
  errorCode = 500,
}: {
  topicName: string;
  errorCode?: number;
}) =>
  rest.get(`${url}/topics/${topicName}/metrics`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchTopicSubscriptionsHandler = ({
  topicName,
  subscriptions = dummyTopicSubscriptionsList,
}: {
  topicName: string;
  subscriptions?: string[];
}) =>
  rest.get(`${url}/topics/${topicName}/subscriptions`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(subscriptions));
  });

export const fetchTopicSubscriptionsErrorHandler = ({
  topicName,
  errorCode = 500,
}: {
  topicName: string;
  errorCode?: number;
}) =>
  rest.get(`${url}/topics/${topicName}/subscriptions`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchTopicSubscriptionDetailsHandler = ({
  subscription = dummySubscription,
}: {
  subscription?: Subscription;
}) =>
  rest.get(
    `${url}/topics/${subscription.topicName}/subscriptions/${subscription.name}`,
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(subscription));
    },
  );

export const fetchTopicSubscriptionDetailsErrorHandler = ({
  topicName,
  subscriptionName,
  errorCode = 500,
}: {
  topicName: string;
  subscriptionName: string;
  errorCode?: number;
}) =>
  rest.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}`,
    (req, res, ctx) => {
      return res(ctx.status(errorCode), ctx.json(undefined));
    },
  );

export const successfulTopicHandlers = [
  fetchTopicHandler({}),
  fetchTopicOwnerHandler({}),
  fetchTopicMessagesPreviewHandler({ topicName: dummyTopic.name }),
  fetchTopicMetricsHandler({ topicName: dummyTopic.name }),
  fetchTopicSubscriptionsHandler({ topicName: dummyTopic.name }),
  fetchTopicSubscriptionDetailsHandler({ subscription: dummySubscription }),
  fetchTopicSubscriptionDetailsHandler({
    subscription: secondDummySubscription,
  }),
];

export const fetchConstraintsHandler = ({
  constraints,
}: {
  constraints: ConstraintsConfig;
}) =>
  rest.get(`${url}/workload-constraints`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(constraints));
  });

export const fetchConstraintsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  rest.get(`${url}/workload-constraints`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchReadinessHandler = ({
  datacentersReadiness,
}: {
  datacentersReadiness: DatacenterReadiness[];
}) =>
  rest.get(`${url}/readiness/datacenters`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(datacentersReadiness));
  });

export const fetchReadinessErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  rest.get(`${url}/readiness/datacenters`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchConsumerGroupsHandler = ({
  consumerGroups,
  topicName,
  subscriptionName,
}: {
  consumerGroups: ConsumerGroup[];
  topicName: string;
  subscriptionName: string;
}) =>
  rest.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/consumer-groups`,
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(consumerGroups));
    },
  );

export const fetchConsumerGroupsErrorHandler = ({
  errorCode = 500,
  topicName,
  subscriptionName,
}: {
  errorCode?: number;
  topicName: string;
  subscriptionName: string;
}) =>
  rest.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/consumer-groups`,
    (req, res, ctx) => {
      return res(ctx.status(errorCode), ctx.json(undefined));
    },
  );

export const fetchInconsistentTopicsHandler = ({
  topics,
}: {
  topics: string[];
}) =>
  rest.get(`${url}/consistency/inconsistencies/topics`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(topics));
  });

export const fetchInconsistentTopicsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  rest.get(`${url}/consistency/inconsistencies/topics`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchTopicNamesHandler = ({
  topicNames,
}: {
  topicNames: string[];
}) =>
  rest.get(`${url}/topics`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(topicNames));
  });

export const fetchTopicNamesErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  rest.get(`${url}/topics`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchGroupNamesHandler = ({
  groupNames,
}: {
  groupNames: string[];
}) =>
  rest.get(`${url}/groups`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(groupNames));
  });

export const fetchGroupNamesErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  rest.get(`${url}/groups`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchStatsHandler = ({ stats }: { stats: Stats }) =>
  rest.get(`${url}/stats`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(stats));
  });

export const fetchStatsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  rest.get(`${url}/stats`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchTokenHandler = ({
  accessToken,
}: {
  accessToken: AccessTokenResponse;
}) =>
  rest.post(`http://localhost:8080/token`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(accessToken));
  });

export const fetchRolesHandler = ({
  roles,
  path,
}: {
  roles: Roles[];
  path: string;
}) =>
  rest.get(path, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(roles));
  });

export const fetchRolesErrorHandler = ({
  errorCode = 500,
  path,
}: {
  errorCode?: number;
  path: string;
}) =>
  rest.get(path, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });
