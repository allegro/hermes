import {
  dummyOwner,
  dummyTopic,
  dummyTopicMessagesPreview,
  dummyTopicMetrics,
} from '@/dummy/topic';
import {
  dummySubscription,
  dummySubscriptionHealth,
  dummySubscriptionMetrics,
  dummyTopicSubscriptionsList,
  dummyUndeliveredMessage,
  dummyUndeliveredMessages,
  secondDummySubscription,
} from '@/dummy/subscription';
import { rest } from 'msw';
import type { AccessTokenResponse } from '@/api/access-token-response';
import type { ConstraintsConfig } from '@/api/constraints';
import type { ConsumerGroup } from '@/api/consumer-group';
import type { DashboardUrl } from '@/composables/metrics/use-metrics/useMetrics';
import type { DatacenterReadiness } from '@/api/datacenter-readiness';
import type { Group } from '@/api/group';
import type { InconsistentGroup } from '@/api/inconsistent-group';
import type { MessageFiltersVerificationResponse } from '@/api/message-filters-verification';
import type {
  MessagePreview,
  Topic,
  TopicMetrics,
  TopicWithSchema,
} from '@/api/topic';
import type { Owner } from '@/api/owner';
import type { Role } from '@/api/role';
import type { SentMessageTrace } from '@/api/subscription-undelivered';
import type { Stats } from '@/api/stats';
import type { Subscription } from '@/api/subscription';
import type { SubscriptionHealth } from '@/api/subscription-health';
import type { SubscriptionMetrics } from '@/api/subscription-metrics';

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

export const fetchOwnerHandler = ({ owner = dummyOwner }: { owner?: Owner }) =>
  rest.get(
    `${url}/owners/sources/Service%20Catalog/${owner.id}`,
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(owner));
    },
  );

export const fetchOwnerSourcesHandler = (body: any) =>
  rest.get(`${url}/owners/sources`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(body));
  });

export const fetchOwnerErrorHandler = ({
  owner,
  errorCode = 500,
}: {
  owner: string;
  errorCode?: number;
}) =>
  rest.get(
    `${url}/owners/sources/Service%20Catalog/${owner}`,
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
  fetchOwnerHandler({}),
  fetchTopicMessagesPreviewHandler({ topicName: dummyTopic.name }),
  fetchTopicMetricsHandler({ topicName: dummyTopic.name }),
  fetchTopicSubscriptionsHandler({ topicName: dummyTopic.name }),
  fetchTopicSubscriptionDetailsHandler({ subscription: dummySubscription }),
  fetchTopicSubscriptionDetailsHandler({
    subscription: secondDummySubscription,
  }),
];

export const fetchSubscriptionHandler = ({
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

export const fetchSubscriptionMetricsHandler = ({
  topicName = dummySubscription.topicName,
  subscriptionName = dummySubscription.name,
  subscriptionMetrics = dummySubscriptionMetrics,
}: {
  topicName?: string;
  subscriptionName?: string;
  subscriptionMetrics?: SubscriptionMetrics;
}) =>
  rest.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/metrics`,
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(subscriptionMetrics));
    },
  );

export const fetchSubscriptionHealthHandler = ({
  topicName = dummySubscription.topicName,
  subscriptionName = dummySubscription.name,
  subscriptionHealth = dummySubscriptionHealth,
}: {
  topicName?: string;
  subscriptionName?: string;
  subscriptionHealth?: SubscriptionHealth;
}) =>
  rest.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/health`,
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(subscriptionHealth));
    },
  );

export const fetchSubscriptionUndeliveredMessagesHandler = ({
  topicName = dummySubscription.topicName,
  subscriptionName = dummySubscription.name,
  subscriptionUndeliveredMessages = dummyUndeliveredMessages,
}: {
  topicName?: string;
  subscriptionName?: string;
  subscriptionUndeliveredMessages?: SentMessageTrace[];
}) =>
  rest.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/undelivered`,
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(subscriptionUndeliveredMessages));
    },
  );

export const fetchSubscriptionLastUndeliveredMessageHandler = ({
  topicName = dummySubscription.topicName,
  subscriptionName = dummySubscription.name,
  subscriptionLastUndeliveredMessage = dummyUndeliveredMessage,
}: {
  topicName?: string;
  subscriptionName?: string;
  subscriptionLastUndeliveredMessage?: SentMessageTrace;
}) =>
  rest.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/undelivered/last`,
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(subscriptionLastUndeliveredMessage));
    },
  );

export const fetchSubscriptionErrorHandler = ({
  subscription = dummySubscription,
  errorCode = 500,
}: {
  subscription?: Subscription;
  errorCode?: number;
}) =>
  rest.get(
    `${url}/topics/${subscription.topicName}/subscriptions/${subscription.name}`,
    (req, res, ctx) => {
      return res(ctx.status(errorCode), ctx.json(undefined));
    },
  );

export const fetchSubscriptionMetricsErrorHandler = ({
  topicName = dummySubscription.topicName,
  subscriptionName = dummySubscription.name,
  errorCode = 500,
}: {
  topicName?: string;
  subscriptionName?: string;
  errorCode?: number;
}) =>
  rest.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/metrics`,
    (req, res, ctx) => {
      return res(ctx.status(errorCode), ctx.json(undefined));
    },
  );

export const fetchSubscriptionHealthErrorHandler = ({
  topicName = dummySubscription.topicName,
  subscriptionName = dummySubscription.name,
  errorCode = 500,
}: {
  topicName?: string;
  subscriptionName?: string;
  errorCode?: number;
}) =>
  rest.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/health`,
    (req, res, ctx) => {
      return res(ctx.status(errorCode), ctx.json(undefined));
    },
  );

export const fetchSubscriptionUndeliveredMessagesErrorHandler = ({
  topicName = dummySubscription.topicName,
  subscriptionName = dummySubscription.name,
  errorCode = 500,
}: {
  topicName?: string;
  subscriptionName?: string;
  errorCode?: number;
}) =>
  rest.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/undelivered`,
    (req, res, ctx) => {
      return res(ctx.status(errorCode), ctx.json(undefined));
    },
  );

export const fetchSubscriptionLastUndeliveredMessageErrorHandler = ({
  topicName = dummySubscription.topicName,
  subscriptionName = dummySubscription.name,
  errorCode = 500,
}: {
  topicName?: string;
  subscriptionName?: string;
  errorCode?: number;
}) =>
  rest.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/undelivered/last`,
    (req, res, ctx) => {
      return res(ctx.status(errorCode), ctx.json(undefined));
    },
  );

export const successfulSubscriptionHandlers = [
  fetchSubscriptionHandler({}),
  fetchOwnerHandler({}),
  fetchSubscriptionMetricsHandler({}),
  fetchSubscriptionHealthHandler({}),
  fetchSubscriptionUndeliveredMessagesHandler({}),
  fetchSubscriptionLastUndeliveredMessageHandler({}),
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

export const queryTopicsHandler = ({
  topics = [dummyTopic],
}: {
  topics?: Topic[];
}) =>
  rest.post(`${url}/query/topics`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(topics));
  });

export const queryTopicsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  rest.post(`${url}/query/topics`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const querySubscriptionsHandler = ({
  subscriptions = [dummySubscription],
}: {
  subscriptions?: Subscription[];
}) =>
  rest.post(`${url}/query/subscriptions`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(subscriptions));
  });

export const querySubscriptionsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  rest.post(`${url}/query/subscriptions`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchRolesHandler = ({
  roles,
  path,
}: {
  roles: Role[];
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

export const fetchMetricsDashboardUrlHandler = ({
  dashboardUrl,
  path,
}: {
  dashboardUrl: DashboardUrl;
  path: string;
}) =>
  rest.get(path, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(dashboardUrl));
  });

export const fetchMetricsDashboardUrlErrorHandler = ({
  errorCode = 500,
  path,
}: {
  errorCode?: number;
  path: string;
}) =>
  rest.get(path, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchConsistencyGroupsHandler = ({
  groups,
}: {
  groups: string[];
}) =>
  rest.get(`${url}/consistency/groups`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(groups));
  });

export const fetchConsistencyGroupsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  rest.get(`${url}/consistency/groups`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const fetchGroupInconsistenciesHandler = ({
  groupsInconsistency,
}: {
  groupsInconsistency: InconsistentGroup[];
}) =>
  rest.get(`${url}/consistency/inconsistencies/groups`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(groupsInconsistency));
  });

export const fetchGroupInconsistenciesErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  rest.get(`${url}/consistency/inconsistencies/groups`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const removeGroupHandler = ({ group }: { group: string }) =>
  rest.delete(`/groups/${group}`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(undefined));
  });

export const removeGroupErrorHandler = ({
  group,
  errorCode = 500,
}: {
  group: string;
  errorCode: number;
}) =>
  rest.delete(`/groups/${group}`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const removeTopicHandler = ({ topic }: { topic: string }) =>
  rest.delete(`/topics/${topic}`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(undefined));
  });

export const removeTopicErrorHandler = ({
  topic,
  errorCode = 500,
}: {
  topic: string;
  errorCode: number;
}) =>
  rest.delete(`/topics/${topic}`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const removeInconsistentTopicHandler = () =>
  rest.delete(`/consistency/inconsistencies/topics`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(undefined));
  });

export const removeInconsistentTopicErrorHandler = ({
  errorCode = 500,
}: {
  errorCode: number;
}) =>
  rest.delete(`/consistency/inconsistencies/topics`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const removeSubscriptionHandler = ({
  topic,
  subscription,
}: {
  topic: string;
  subscription: string;
}) =>
  rest.delete(
    `/topics/${topic}/subscriptions/${subscription}`,
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(undefined));
    },
  );

export const removeSubscriptionErrorHandler = ({
  topic,
  subscription,
  errorCode = 500,
}: {
  topic: string;
  subscription: string;
  errorCode: number;
}) =>
  rest.delete(
    `/topics/${topic}/subscriptions/${subscription}`,
    (req, res, ctx) => {
      return res(ctx.status(errorCode), ctx.json(undefined));
    },
  );

export const subscriptionStateHandler = ({
  topic,
  subscription,
}: {
  topic: string;
  subscription: string;
}) =>
  rest.put(
    `/topics/${topic}/subscriptions/${subscription}/state`,
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(undefined));
    },
  );

export const subscriptionStateErrorHandler = ({
  topic,
  subscription,
  errorCode = 500,
}: {
  topic: string;
  subscription: string;
  errorCode: number;
}) =>
  rest.put(
    `/topics/${topic}/subscriptions/${subscription}/state`,
    (req, res, ctx) => {
      return res(ctx.status(errorCode), ctx.json(undefined));
    },
  );

export const switchReadinessHandler = ({
  datacenter,
}: {
  datacenter: string;
}) =>
  rest.post(`/readiness/datacenters/${datacenter}`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(undefined));
  });

export const switchReadinessErrorHandler = ({
  datacenter,
  errorCode,
}: {
  datacenter: string;
  errorCode: number;
}) =>
  rest.post(`/readiness/datacenters/${datacenter}`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const moveSubscriptionOffsetsHandler = ({
  topicName,
  subscriptionName,
  statusCode,
}: {
  topicName: string;
  subscriptionName: string;
  statusCode: number;
}) =>
  rest.post(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/moveOffsetsToTheEnd`,
    (req, res, ctx) => {
      return res(ctx.status(statusCode), ctx.json(undefined));
    },
  );

export const upsertTopicConstraintHandler = ({
  statusCode,
}: {
  statusCode: number;
}) =>
  rest.put(`${url}/workload-constraints/topic`, (req, res, ctx) => {
    return res(ctx.status(statusCode), ctx.json(undefined));
  });

export const upsertSubscriptionConstraintHandler = ({
  statusCode,
}: {
  statusCode: number;
}) =>
  rest.put(`${url}/workload-constraints/subscription`, (req, res, ctx) => {
    return res(ctx.status(statusCode), ctx.json(undefined));
  });

export const deleteTopicConstraintHandler = ({
  statusCode,
  topicName,
}: {
  statusCode: number;
  topicName: string;
}) =>
  rest.delete(
    `${url}/workload-constraints/topic/${topicName}`,
    (req, res, ctx) => {
      return res(ctx.status(statusCode), ctx.json(undefined));
    },
  );

export const deleteSubscriptionConstraintHandler = ({
  statusCode,
  topicName,
  subscriptionName,
}: {
  statusCode: number;
  topicName: string;
  subscriptionName: string;
}) =>
  rest.delete(
    `${url}/workload-constraints/subscription/${topicName}/${subscriptionName}`,
    (req, res, ctx) => {
      return res(ctx.status(statusCode), ctx.json(undefined));
    },
  );

export const createSubscriptionHandler = (topic: string) =>
  rest.post(`${url}/topics/${topic}/subscriptions`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(undefined));
  });

export const createSubscriptionErrorHandler = (
  topic: string,
  errorCode: number,
) =>
  rest.post(`${url}/topics/${topic}/subscriptions`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const createTopicHandler = () =>
  rest.post(`${url}/topics`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(undefined));
  });

export const createTopicErrorHandler = (errorCode: number) =>
  rest.post(`${url}/topics`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const editSubscriptionHandler = (topic: string, subscription: string) =>
  rest.put(
    `${url}/topics/${topic}/subscriptions/${subscription}`,
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(undefined));
    },
  );

export const editSubscriptionErrorHandler = (
  topic: string,
  subscription: string,
  errorCode: number,
) =>
  rest.put(
    `${url}/topics/${topic}/subscriptions/${subscription}`,
    (req, res, ctx) => {
      return res(ctx.status(errorCode), ctx.json(undefined));
    },
  );

export const editTopicHandler = (topic: string) =>
  rest.put(`${url}/topics/${topic}`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(undefined));
  });

export const editTopicErrorHandler = (topic: string, errorCode: number) =>
  rest.put(`${url}/topics/${topic}`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const createGroupHandler = ({ group }: { group: Group }) =>
  rest.post(`${url}/groups`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(group));
  });

export const createGroupErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  rest.post(`${url}/groups`, (req, res, ctx) => {
    return res(ctx.status(errorCode), ctx.json(undefined));
  });

export const createRetransmissionTaskHandler = ({
  statusCode,
}: {
  statusCode: number;
}) =>
  rest.post(`${url}/offline-retransmission/tasks`, (req, res, ctx) => {
    return res(ctx.status(statusCode), ctx.json(undefined));
  });

export const createRetransmissionHandler = ({
  statusCode,
  topicName,
  subscriptionName,
}: {
  statusCode: number;
  topicName: string;
  subscriptionName: string;
}) =>
  rest.put(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/retransmission`,
    (req, res, ctx) => {
      return res(ctx.status(statusCode), ctx.json(undefined));
    },
  );

export const subscriptionFilterVerificationHandler = ({
  topicName,
  response,
}: {
  topicName: string;
  response: MessageFiltersVerificationResponse;
}) =>
  rest.post(`${url}/filters/${topicName}`, (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(response));
  });

export const subscriptionFilterVerificationErrorHandler = ({
  topicName,
}: {
  topicName: string;
}) =>
  rest.post(`${url}/filters/${topicName}`, (req, res, ctx) => {
    return res(ctx.status(500), ctx.json(undefined));
  });
