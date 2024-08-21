import { parseTopicForm } from '@/composables/topic/use-form-topic/parser';
import { State } from '@/api/subscription';
import axios from '@/utils/axios/axios-instance';
import qs from 'query-string';
import type { AccessTokenResponse } from '@/api/access-token-response';
import type { AppConfiguration } from '@/api/app-configuration';
import type { AxiosRequestConfig } from 'axios';
import type {
  Constraint,
  ConstraintsConfig,
  SubscriptionConstraint,
  TopicConstraint,
} from '@/api/constraints';
import type { ConsumerGroup } from '@/api/consumer-group';
import type {
  CreateSubscriptionFormRequestBody,
  Subscription,
} from '@/api/subscription';
import type { DashboardUrl } from '@/composables/metrics/use-metrics/use-metrics';
import type {
  DatacenterReadiness,
  Readiness,
} from '@/api/datacenter-readiness';
import type { Group } from '@/api/group';
import type { InconsistentGroup } from '@/api/inconsistent-group';
import type {
  MessageFiltersVerification,
  MessageFiltersVerificationResponse,
} from '@/api/message-filters-verification';
import type {
  MessagePreview,
  Topic,
  TopicMetrics,
  TopicWithSchema,
} from '@/api/topic';
import type { OfflineClientsSource } from '@/api/offline-clients-source';
import type { OfflineRetransmissionTask } from '@/api/offline-retransmission';
import type { Owner, OwnerSource } from '@/api/owner';
import type { ResponsePromise } from '@/utils/axios/axios-utils';
import type { RetransmissionDate } from '@/api/OffsetRetransmissionDate';
import type { Role } from '@/api/role';
import type { SentMessageTrace } from '@/api/subscription-undelivered';
import type { Stats } from '@/api/stats';
import type { SubscriptionHealth } from '@/api/subscription-health';
import type { SubscriptionMetrics } from '@/api/subscription-metrics';
import type { TopicForm } from '@/composables/topic/use-form-topic/types';

const acceptHeader = 'Accept';
const contentTypeHeader = 'Content-Type';
const applicationJsonMediaType = 'application/json';

export function fetchTopic(
  topicName: string,
): ResponsePromise<TopicWithSchema> {
  return axios.get<TopicWithSchema>(`/topics/${topicName}`);
}

export function fetchOwner(
  ownerId: string,
  source: string,
): ResponsePromise<Owner> {
  return axios.get<Owner>(`/owners/sources/${source}/${ownerId}`);
}

export function fetchOwnersSources(): ResponsePromise<OwnerSource[]> {
  return axios.get<OwnerSource[]>(`/owners/sources`);
}

export function searchOwners(
  source: string,
  searchPhrase: string,
): ResponsePromise<Owner[]> {
  return axios.get<Owner[]>(`/owners/sources/${source}?search=${searchPhrase}`);
}

export function fetchTopicMessagesPreview(
  topicName: string,
): ResponsePromise<MessagePreview[]> {
  return axios.get<MessagePreview[]>(`/topics/${topicName}/preview`);
}

export function fetchTopicMetrics(
  topic: String,
): ResponsePromise<TopicMetrics> {
  return axios.get<TopicMetrics>(`/topics/${topic}/metrics`);
}

export function fetchTopicSubscriptions(
  topicName: string,
): ResponsePromise<string[]> {
  return axios.get<string[]>(`/topics/${topicName}/subscriptions`);
}

export function fetchTopicSubscriptionDetails(
  topicName: string,
  subscription: string,
): ResponsePromise<Subscription> {
  return axios.get<Subscription>(
    `/topics/${topicName}/subscriptions/${subscription}`,
  );
}

export function fetchSubscription(
  topicName: string,
  subscriptionName: string,
): ResponsePromise<Subscription> {
  return axios.get<Subscription>(
    `/topics/${topicName}/subscriptions/${subscriptionName}`,
  );
}

export function suspendSubscription(
  topicName: string,
  subscriptionName: string,
): ResponsePromise<void> {
  return axios.put(
    `/topics/${topicName}/subscriptions/${subscriptionName}/state`,
    State.SUSPENDED,
    {
      headers: {
        [contentTypeHeader]: applicationJsonMediaType,
      },
    },
  );
}

export function activateSubscription(
  topicName: string,
  subscriptionName: string,
): ResponsePromise<void> {
  return axios.put(
    `/topics/${topicName}/subscriptions/${subscriptionName}/state`,
    State.ACTIVE,
    {
      headers: {
        [contentTypeHeader]: applicationJsonMediaType,
      },
    },
  );
}

export function fetchSubscriptionMetrics(
  topicName: string,
  subscriptionName: string,
): ResponsePromise<SubscriptionMetrics> {
  return axios.get<SubscriptionMetrics>(
    `/topics/${topicName}/subscriptions/${subscriptionName}/metrics`,
  );
}

export function fetchSubscriptionHealth(
  topicName: string,
  subscriptionName: string,
): ResponsePromise<SubscriptionHealth> {
  return axios.get<SubscriptionHealth>(
    `/topics/${topicName}/subscriptions/${subscriptionName}/health`,
  );
}

export function fetchSubscriptionUndeliveredMessages(
  topicName: string,
  subscriptionName: string,
): ResponsePromise<SentMessageTrace[]> {
  return axios.get<SentMessageTrace[]>(
    `/topics/${topicName}/subscriptions/${subscriptionName}/undelivered`,
  );
}

export function fetchSubscriptionLastUndeliveredMessage(
  topicName: string,
  subscriptionName: string,
): ResponsePromise<SentMessageTrace> {
  return axios.get<SentMessageTrace>(
    `/topics/${topicName}/subscriptions/${subscriptionName}/undelivered/last`,
  );
}

export function fetchAppConfiguration(): ResponsePromise<AppConfiguration> {
  return axios.get<AppConfiguration>('/console', {
    headers: { accept: 'application/json' },
  });
}

export function fetchOfflineClientsSource(
  topicName: string,
): ResponsePromise<OfflineClientsSource> {
  return axios.get<OfflineClientsSource>(
    `/topics/${topicName}/offline-clients-source`,
  );
}

export function fetchConstraints(): ResponsePromise<ConstraintsConfig> {
  return axios.get<ConstraintsConfig>('/workload-constraints');
}

export function fetchReadiness(): ResponsePromise<DatacenterReadiness[]> {
  return axios.get<DatacenterReadiness[]>('/readiness/datacenters');
}

export function fetchConsumerGroups(
  topicName: string,
  subscription: string,
): ResponsePromise<ConsumerGroup[]> {
  return axios.get<ConsumerGroup[]>(
    `/topics/${topicName}/subscriptions/${subscription}/consumer-groups`,
  );
}

export function fetchConsistencyGroups(): ResponsePromise<string[]> {
  return axios.get<string[]>('/consistency/groups');
}

export function fetchInconsistentGroups(
  groups: string[],
): ResponsePromise<InconsistentGroup[]> {
  return axios.get<InconsistentGroup[]>('/consistency/inconsistencies/groups', {
    params: {
      groupNames: groups,
    },
    paramsSerializer: {
      indexes: null,
    },
  });
}

export function fetchInconsistentTopics(): ResponsePromise<string[]> {
  return axios.get<string[]>('/consistency/inconsistencies/topics');
}

export function fetchTopicNames(): ResponsePromise<string[]> {
  return axios.get<string[]>('/topics');
}

export function fetchGroupNames(): ResponsePromise<string[]> {
  return axios.get<string[]>('/groups');
}

export function fetchStats(): ResponsePromise<Stats> {
  return axios.get<Stats>(`/stats`);
}

export function fetchToken(
  code: string,
  url: string,
  clientId: string,
  redirectUri: string,
  codeVerifier: string,
): ResponsePromise<AccessTokenResponse> {
  return axios.post<AccessTokenResponse>(
    url,
    qs.stringify({
      client_id: clientId,
      code: code,
      redirect_uri: redirectUri,
      grant_type: 'authorization_code',
      code_verifier: codeVerifier,
    }),
    {
      'Content-Type': 'application/x-www-form-urlencoded',
    } as AxiosRequestConfig,
  );
}

export function queryTopics(queryJSON: object): ResponsePromise<Topic[]> {
  return axios.post<Topic[]>(`/query/topics`, queryJSON, {
    headers: { 'Content-Type': 'application/json' },
  });
}

export function querySubscriptions(
  queryJSON: object,
): ResponsePromise<Subscription[]> {
  return axios.post<Subscription[]>(`/query/subscriptions`, queryJSON, {
    headers: { 'Content-Type': 'application/json' },
  });
}

export function fetchRoles(path: string): ResponsePromise<Role[]> {
  return axios.get<Role[]>(path);
}

export function fetchDashboardUrl(path: string): ResponsePromise<DashboardUrl> {
  return axios.get<DashboardUrl>(path);
}

export function moveSubscriptionOffsets(
  topicName: string,
  subscription: string,
): ResponsePromise<null> {
  return axios.post<null>(
    `/topics/${topicName}/subscriptions/${subscription}/moveOffsetsToTheEnd`,
  );
}

export function removeTopic(topic: String): ResponsePromise<void> {
  return axios.delete(`/topics/${topic}`);
}

export function removeSubscription(
  topic: String,
  subscription: String,
): ResponsePromise<void> {
  return axios.delete(`/topics/${topic}/subscriptions/${subscription}`);
}

export function removeGroup(group: String): ResponsePromise<void> {
  return axios.delete(`/groups/${group}`);
}

export function removeInconsistentTopic(topic: string): ResponsePromise<void> {
  return axios.delete('/consistency/inconsistencies/topics', {
    params: {
      topicName: topic,
    },
    paramsSerializer: {
      indexes: null,
    },
  });
}

export function switchReadiness(
  datacenter: string,
  readiness: Readiness,
): ResponsePromise<void> {
  return axios.post(`/readiness/datacenters/${datacenter}`, readiness, {
    headers: {
      [contentTypeHeader]: applicationJsonMediaType,
    },
  });
}

export function upsertTopicConstraint(
  topicName: string,
  constraints: Constraint,
): ResponsePromise<void> {
  const body: TopicConstraint = {
    topicName: topicName,
    constraints: constraints,
  };
  return axios.put(`/workload-constraints/topic`, body, {
    headers: { 'Content-Type': 'application/json' },
  });
}

export function deleteTopicConstraint(
  topicName: string,
): ResponsePromise<void> {
  return axios.delete(`/workload-constraints/topic/${topicName}`);
}

export function upsertSubscriptionConstraint(
  subscriptionName: string,
  constraints: Constraint,
): ResponsePromise<void> {
  const body: SubscriptionConstraint = {
    subscriptionName: subscriptionName,
    constraints: constraints,
  };
  return axios.put(`/workload-constraints/subscription`, body, {
    headers: { 'Content-Type': 'application/json' },
  });
}

export function deleteSubscriptionConstraint(
  topicName: string,
  subscriptionName: string,
): ResponsePromise<void> {
  return axios.delete(
    `/workload-constraints/subscription/${topicName}/${subscriptionName}`,
  );
}

export function createGroup(group: Group): ResponsePromise<void> {
  return axios.post(`/groups`, group, {
    headers: { 'Content-Type': 'application/json' },
  });
}

export function retransmitSubscriptionMessages(
  topicName: string,
  subscriptionName: string,
  retransmissionDate: RetransmissionDate,
) {
  return axios.put(
    `/topics/${topicName}/subscriptions/${subscriptionName}/retransmission`,
    retransmissionDate,
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: 60 * 1000,
    },
  );
}

export function createRetransmissionTask(task: OfflineRetransmissionTask) {
  return axios.post(`/offline-retransmission/tasks`, task, {
    headers: { 'Content-Type': 'application/json' },
  });
}

export function createSubscription(
  topic: string,
  requestBody: CreateSubscriptionFormRequestBody,
): ResponsePromise<void> {
  return axios.post(`/topics/${topic}/subscriptions`, requestBody, {
    headers: {
      [acceptHeader]: applicationJsonMediaType,
      [contentTypeHeader]: applicationJsonMediaType,
    },
  });
}

export function editSubscription(
  topic: string,
  subscription: string,
  requestBody: CreateSubscriptionFormRequestBody,
): ResponsePromise<void> {
  return axios.put(
    `/topics/${topic}/subscriptions/${subscription}`,
    requestBody,
    {
      headers: {
        [acceptHeader]: applicationJsonMediaType,
        [contentTypeHeader]: applicationJsonMediaType,
      },
    },
  );
}

export function createTopic(
  topicForm: TopicForm,
  group: string,
): ResponsePromise<void> {
  const parsedRequestBody = parseTopicForm(topicForm, group);
  return axios.post(`/topics`, parsedRequestBody, {
    headers: {
      [acceptHeader]: applicationJsonMediaType,
      [contentTypeHeader]: applicationJsonMediaType,
    },
  });
}

export function editTopic(topicForm: TopicForm): ResponsePromise<void> {
  const parsedRequestBody = parseTopicForm(topicForm, null);
  return axios.put(`/topics/${topicForm.name}`, parsedRequestBody, {
    headers: {
      [acceptHeader]: applicationJsonMediaType,
      [contentTypeHeader]: applicationJsonMediaType,
    },
  });
}

export function verifyFilters(
  topic: string,
  verification: MessageFiltersVerification,
): ResponsePromise<MessageFiltersVerificationResponse> {
  return axios.post<MessageFiltersVerificationResponse>(
    `/filters/${topic}`,
    verification,
    {
      headers: {
        [acceptHeader]: applicationJsonMediaType,
        [contentTypeHeader]: applicationJsonMediaType,
      },
    },
  );
}

export function syncGroup(
  groupName: string,
  primaryDatacenter: string,
): ResponsePromise<void> {
  return axios.post<void>(`/consistency/sync/groups/${groupName}`, null, {
    params: {
      primaryDatacenter: primaryDatacenter,
    },
  });
}

export function syncTopic(
  topicQualifiedName: string,
  primaryDatacenter: string,
): ResponsePromise<void> {
  return axios.post<void>(
    `/consistency/sync/topics/${topicQualifiedName}`,
    null,
    {
      params: {
        primaryDatacenter: primaryDatacenter,
      },
    },
  );
}

export function syncSubscription(
  topicQualifiedName: string,
  subscriptionName: string,
  primaryDatacenter: string,
): ResponsePromise<void> {
  return axios.post<void>(
    `/consistency/sync/topics/${topicQualifiedName}/subscriptions/${subscriptionName}`,
    null,
    {
      params: {
        primaryDatacenter: primaryDatacenter,
      },
    },
  );
}
