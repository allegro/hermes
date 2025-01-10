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
import { http, HttpResponse } from 'msw';
import type { AccessTokenResponse } from '@/api/access-token-response';
import type { ConstraintsConfig } from '@/api/constraints';
import type { ConsumerGroup } from '@/api/consumer-group';
import type { DashboardUrl } from '@/composables/metrics/use-metrics/useMetrics';
import type { DatacenterReadiness } from '@/api/datacenter-readiness';
import type { Group } from '@/api/group';
import type { InactiveTopic } from '@/api/inactive-topics';
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
  http.get(`${url}/topics/${topic.name}`, () => {
    return HttpResponse.json(topic);
  });

export const fetchTopicErrorHandler = ({
  topicName,
  errorCode = 500,
}: {
  topicName: string;
  errorCode?: number;
}) =>
  http.get(`${url}/topics/${topicName}`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchOwnerHandler = ({ owner = dummyOwner }: { owner?: Owner }) =>
  http.get(`${url}/owners/sources/Service%20Catalog/${owner.id}`, () => {
    return HttpResponse.json(owner);
  });

export const fetchOwnerSourcesHandler = (body: any) =>
  http.get(`${url}/owners/sources`, () => {
    return HttpResponse.json(body);
  });

export const fetchOwnerErrorHandler = ({
  owner,
  errorCode = 500,
}: {
  owner: string;
  errorCode?: number;
}) =>
  http.get(`${url}/owners/sources/Service%20Catalog/${owner}`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchTopicMessagesPreviewHandler = ({
  topicName,
  messages = dummyTopicMessagesPreview,
}: {
  topicName: string;
  messages?: MessagePreview[];
}) =>
  http.get(`${url}/topics/${topicName}/preview`, () => {
    return HttpResponse.json(messages);
  });

export const fetchTopicMessagesPreviewErrorHandler = ({
  topicName,
  errorCode = 500,
}: {
  topicName: string;
  errorCode?: number;
}) =>
  http.get(`${url}/topics/${topicName}/preview`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchTopicMetricsHandler = ({
  topicName,
  metrics = dummyTopicMetrics,
}: {
  topicName: string;
  metrics?: TopicMetrics;
}) =>
  http.get(`${url}/topics/${topicName}/metrics`, () => {
    return HttpResponse.json(metrics);
  });

export const fetchTopicMetricsErrorHandler = ({
  topicName,
  errorCode = 500,
}: {
  topicName: string;
  errorCode?: number;
}) =>
  http.get(`${url}/topics/${topicName}/metrics`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchTopicSubscriptionsHandler = ({
  topicName,
  subscriptions = dummyTopicSubscriptionsList,
}: {
  topicName: string;
  subscriptions?: string[];
}) =>
  http.get(`${url}/topics/${topicName}/subscriptions`, () => {
    return HttpResponse.json(subscriptions);
  });

export const fetchTopicSubscriptionsErrorHandler = ({
  topicName,
  errorCode = 500,
}: {
  topicName: string;
  errorCode?: number;
}) =>
  http.get(`${url}/topics/${topicName}/subscriptions`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchTopicSubscriptionDetailsHandler = ({
  subscription = dummySubscription,
}: {
  subscription?: Subscription;
}) =>
  http.get(
    `${url}/topics/${subscription.topicName}/subscriptions/${subscription.name}`,
    () => {
      return HttpResponse.json(subscription);
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
  http.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}`,
    () => {
      return new HttpResponse(undefined, {
        status: errorCode,
      });
    },
  );

export const fetchTopicClientsErrorHandler = ({
  topicName,
  errorCode = 500,
}: {
  topicName: string;
  errorCode?: number;
}) =>
  http.get(`${url}/topics/${topicName}/clients`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

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
  http.get(
    `${url}/topics/${subscription.topicName}/subscriptions/${subscription.name}`,
    () => {
      return HttpResponse.json(subscription);
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
  http.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/metrics`,
    () => {
      return HttpResponse.json(subscriptionMetrics);
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
  http.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/health`,
    () => {
      return HttpResponse.json(subscriptionHealth);
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
  http.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/undelivered`,
    () => {
      return HttpResponse.json(subscriptionUndeliveredMessages);
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
  http.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/undelivered/last`,
    () => {
      return HttpResponse.json(subscriptionLastUndeliveredMessage);
    },
  );

export const fetchSubscriptionErrorHandler = ({
  subscription = dummySubscription,
  errorCode = 500,
}: {
  subscription?: Subscription;
  errorCode?: number;
}) =>
  http.get(
    `${url}/topics/${subscription.topicName}/subscriptions/${subscription.name}`,
    () => {
      return new HttpResponse(undefined, {
        status: errorCode,
      });
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
  http.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/metrics`,
    () => {
      return new HttpResponse(undefined, {
        status: errorCode,
      });
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
  http.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/health`,
    () => {
      return new HttpResponse(undefined, {
        status: errorCode,
      });
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
  http.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/undelivered`,
    () => {
      return new HttpResponse(undefined, {
        status: errorCode,
      });
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
  http.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/undelivered/last`,
    () => {
      return new HttpResponse(undefined, {
        status: errorCode,
      });
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
  http.get(`${url}/workload-constraints`, () => {
    return HttpResponse.json(constraints);
  });

export const fetchConstraintsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  http.get(`${url}/workload-constraints`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchInactiveTopicsHandler = ({
  inactiveTopics,
}: {
  inactiveTopics: InactiveTopic[];
}) =>
  http.get(`${url}/inactive-topics`, () => {
    return HttpResponse.json(inactiveTopics);
  });

export const fetchInactiveTopicsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  http.get(`${url}/inactive-topics`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchReadinessHandler = ({
  datacentersReadiness,
}: {
  datacentersReadiness: DatacenterReadiness[];
}) =>
  http.get(`${url}/readiness/datacenters`, () => {
    return HttpResponse.json(datacentersReadiness);
  });

export const fetchReadinessErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  http.get(`${url}/readiness/datacenters`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
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
  http.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/consumer-groups`,
    () => {
      return HttpResponse.json(consumerGroups);
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
  http.get(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/consumer-groups`,
    () => {
      return new HttpResponse(undefined, {
        status: errorCode,
      });
    },
  );

export const fetchInconsistentTopicsHandler = ({
  topics,
}: {
  topics: string[];
}) =>
  http.get(`${url}/consistency/inconsistencies/topics`, () => {
    return HttpResponse.json(topics);
  });

export const fetchInconsistentTopicsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  http.get(`${url}/consistency/inconsistencies/topics`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchTopicNamesHandler = ({
  topicNames,
}: {
  topicNames: string[];
}) =>
  http.get(`${url}/topics`, () => {
    return HttpResponse.json(topicNames);
  });

export const fetchTopicNamesErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  http.get(`${url}/topics`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchGroupNamesHandler = ({
  groupNames,
}: {
  groupNames: string[];
}) =>
  http.get(`${url}/groups`, () => {
    return HttpResponse.json(groupNames);
  });

export const fetchGroupNamesErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  http.get(`${url}/groups`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchStatsHandler = ({ stats }: { stats: Stats }) =>
  http.get(`${url}/stats`, () => {
    return HttpResponse.json(stats);
  });

export const fetchStatsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  http.get(`${url}/stats`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchTokenHandler = ({
  accessToken,
}: {
  accessToken: AccessTokenResponse;
}) =>
  http.post(`http://localhost:8080/token`, () => {
    return HttpResponse.json(accessToken);
  });

export const queryTopicsHandler = ({
  topics = [dummyTopic],
}: {
  topics?: Topic[];
}) =>
  http.post(`${url}/query/topics`, () => {
    return HttpResponse.json(topics);
  });

export const queryTopicsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  http.post(`${url}/query/topics`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const querySubscriptionsHandler = ({
  subscriptions = [dummySubscription],
}: {
  subscriptions?: Subscription[];
}) =>
  http.post(`${url}/query/subscriptions`, () => {
    return HttpResponse.json(subscriptions);
  });

export const querySubscriptionsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  http.post(`${url}/query/subscriptions`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchRolesHandler = ({
  roles,
  path,
}: {
  roles: Role[];
  path: string;
}) =>
  http.get(path, () => {
    return HttpResponse.json(roles);
  });

export const fetchRolesErrorHandler = ({
  errorCode = 500,
  path,
}: {
  errorCode?: number;
  path: string;
}) =>
  http.get(path, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchMetricsDashboardUrlHandler = ({
  dashboardUrl,
  path,
}: {
  dashboardUrl: DashboardUrl;
  path: string;
}) =>
  http.get(path, () => {
    return HttpResponse.json(dashboardUrl);
  });

export const fetchMetricsDashboardUrlErrorHandler = ({
  errorCode = 500,
  path,
}: {
  errorCode?: number;
  path: string;
}) =>
  http.get(path, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchConsistencyGroupsHandler = ({
  groups,
}: {
  groups: string[];
}) =>
  http.get(`${url}/consistency/groups`, () => {
    return HttpResponse.json(groups);
  });

export const fetchConsistencyGroupsErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  http.get(`${url}/consistency/groups`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const fetchGroupInconsistenciesHandler = ({
  groupsInconsistency,
}: {
  groupsInconsistency: InconsistentGroup[];
}) =>
  http.get(`${url}/consistency/inconsistencies/groups`, () => {
    return HttpResponse.json(groupsInconsistency);
  });

export const fetchGroupInconsistenciesErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  http.get(`${url}/consistency/inconsistencies/groups`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const removeGroupHandler = ({ group }: { group: string }) =>
  http.delete(`/groups/${group}`, () => {
    return HttpResponse.json(undefined);
  });

export const removeGroupErrorHandler = ({
  group,
  errorCode = 500,
}: {
  group: string;
  errorCode: number;
}) =>
  http.delete(`/groups/${group}`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const removeTopicHandler = ({ topic }: { topic: string }) =>
  http.delete(`/topics/${topic}`, () => {
    return HttpResponse.json(undefined);
  });

export const removeTopicErrorHandler = ({
  topic,
  errorCode = 500,
}: {
  topic: string;
  errorCode: number;
}) =>
  http.delete(`/topics/${topic}`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const removeInconsistentTopicHandler = () =>
  http.delete(`/consistency/inconsistencies/topics`, () => {
    return HttpResponse.json(undefined);
  });

export const removeInconsistentTopicErrorHandler = ({
  errorCode = 500,
}: {
  errorCode: number;
}) =>
  http.delete(`/consistency/inconsistencies/topics`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const removeSubscriptionHandler = ({
  topic,
  subscription,
}: {
  topic: string;
  subscription: string;
}) =>
  http.delete(`/topics/${topic}/subscriptions/${subscription}`, () => {
    return HttpResponse.json(undefined);
  });

export const removeSubscriptionErrorHandler = ({
  topic,
  subscription,
  errorCode = 500,
}: {
  topic: string;
  subscription: string;
  errorCode: number;
}) =>
  http.delete(`/topics/${topic}/subscriptions/${subscription}`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const subscriptionStateHandler = ({
  topic,
  subscription,
}: {
  topic: string;
  subscription: string;
}) =>
  http.put(`/topics/${topic}/subscriptions/${subscription}/state`, () => {
    return HttpResponse.json(undefined);
  });

export const subscriptionStateErrorHandler = ({
  topic,
  subscription,
  errorCode = 500,
}: {
  topic: string;
  subscription: string;
  errorCode: number;
}) =>
  http.put(`/topics/${topic}/subscriptions/${subscription}/state`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const switchReadinessHandler = ({
  datacenter,
}: {
  datacenter: string;
}) =>
  http.post(`/readiness/datacenters/${datacenter}`, () => {
    return HttpResponse.json(undefined);
  });

export const switchReadinessErrorHandler = ({
  datacenter,
  errorCode,
}: {
  datacenter: string;
  errorCode: number;
}) =>
  http.post(`/readiness/datacenters/${datacenter}`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const upsertTopicConstraintHandler = ({
  statusCode,
}: {
  statusCode: number;
}) =>
  http.put(`${url}/workload-constraints/topic`, () => {
    return new HttpResponse(undefined, {
      status: statusCode,
    });
  });

export const upsertSubscriptionConstraintHandler = ({
  statusCode,
}: {
  statusCode: number;
}) =>
  http.put(`${url}/workload-constraints/subscription`, () => {
    return new HttpResponse(undefined, {
      status: statusCode,
    });
  });

export const deleteTopicConstraintHandler = ({
  statusCode,
  topicName,
}: {
  statusCode: number;
  topicName: string;
}) =>
  http.delete(`${url}/workload-constraints/topic/${topicName}`, () => {
    return new HttpResponse(undefined, {
      status: statusCode,
    });
  });

export const deleteSubscriptionConstraintHandler = ({
  statusCode,
  topicName,
  subscriptionName,
}: {
  statusCode: number;
  topicName: string;
  subscriptionName: string;
}) =>
  http.delete(
    `${url}/workload-constraints/subscription/${topicName}/${subscriptionName}`,
    () => {
      return new HttpResponse(undefined, {
        status: statusCode,
      });
    },
  );

export const createSubscriptionHandler = (topic: string) =>
  http.post(`${url}/topics/${topic}/subscriptions`, () => {
    return HttpResponse.json(undefined);
  });

export const createSubscriptionErrorHandler = (
  topic: string,
  errorCode: number,
) =>
  http.post(`${url}/topics/${topic}/subscriptions`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const createTopicHandler = () =>
  http.post(`${url}/topics`, () => {
    return HttpResponse.json(undefined);
  });

export const createTopicErrorHandler = (errorCode: number) =>
  http.post(`${url}/topics`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const editSubscriptionHandler = (topic: string, subscription: string) =>
  http.put(`${url}/topics/${topic}/subscriptions/${subscription}`, () => {
    return HttpResponse.json(undefined);
  });

export const editSubscriptionErrorHandler = (
  topic: string,
  subscription: string,
  errorCode: number,
) =>
  http.put(`${url}/topics/${topic}/subscriptions/${subscription}`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const editTopicHandler = (topic: string) =>
  http.put(`${url}/topics/${topic}`, () => {
    return HttpResponse.json(undefined);
  });

export const editTopicErrorHandler = (topic: string, errorCode: number) =>
  http.put(`${url}/topics/${topic}`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const createGroupHandler = ({ group }: { group: Group }) =>
  http.post(`${url}/groups`, () => {
    return HttpResponse.json(group);
  });

export const createGroupErrorHandler = ({
  errorCode = 500,
}: {
  errorCode?: number;
}) =>
  http.post(`${url}/groups`, () => {
    return new HttpResponse(undefined, {
      status: errorCode,
    });
  });

export const createRetransmissionTaskHandler = ({
  statusCode,
}: {
  statusCode: number;
}) =>
  http.post(`${url}/offline-retransmission/tasks`, () => {
    return new HttpResponse(undefined, {
      status: statusCode,
    });
  });

export const createRetransmissionHandler = ({
  statusCode,
  topicName,
  subscriptionName,
  delayMs,
}: {
  statusCode: number;
  topicName: string;
  subscriptionName: string;
  delayMs?: number;
}) =>
  http.put(
    `${url}/topics/${topicName}/subscriptions/${subscriptionName}/retransmission`,
    async () => {
      if (delayMs && delayMs > 0) {
        await new Promise((resolve) => setTimeout(resolve, delayMs));
      }
      return new HttpResponse(undefined, {
        status: statusCode,
      });
    },
  );

export const subscriptionFilterVerificationHandler = ({
  topicName,
  response,
}: {
  topicName: string;
  response: MessageFiltersVerificationResponse;
}) =>
  http.post(`${url}/filters/${topicName}`, () => {
    return HttpResponse.json(response);
  });

export const subscriptionFilterVerificationErrorHandler = ({
  topicName,
}: {
  topicName: string;
}) =>
  http.post(`${url}/filters/${topicName}`, () => {
    return new HttpResponse(undefined, {
      status: 500,
    });
  });

export const syncGroupHandler = ({
  groupName,
  statusCode,
}: {
  groupName: string;
  statusCode: number;
}) =>
  http.post(`${url}/consistency/sync/groups/${groupName}`, () => {
    return new HttpResponse(undefined, {
      status: statusCode,
    });
  });

export const syncTopicHandler = ({
  topicName,
  statusCode,
}: {
  topicName: string;
  statusCode: number;
}) =>
  http.post(`${url}/consistency/sync/topics/${topicName}`, () => {
    return new HttpResponse(undefined, {
      status: statusCode,
    });
  });

export const syncSubscriptionHandler = ({
  topicName,
  statusCode,
  subscriptionName,
}: {
  topicName: string;
  subscriptionName: string;
  statusCode: number;
}) =>
  http.post(
    `${url}/consistency/sync/topics/${topicName}/subscriptions/${subscriptionName}`,
    () => {
      return new HttpResponse(undefined, {
        status: statusCode,
      });
    },
  );
