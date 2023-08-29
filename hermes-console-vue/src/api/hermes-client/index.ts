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
import type { DatacenterReadiness } from '@/api/datacenter-readiness';
import type { InconsistentGroup } from '@/api/inconsistent-group';
import type {
  MessagePreview,
  Topic,
  TopicMetrics,
  TopicWithSchema,
} from '@/api/topic';
import type { OfflineClientsSource } from '@/api/offline-clients-source';
import type { Owner } from '@/api/owner';
import type { ResponsePromise } from '@/utils/axios/axios-utils';
import type { Role } from '@/api/role';
import type { SentMessageTrace } from '@/api/subscription-undelivered';
import type { Stats } from '@/api/stats';
import type { Subscription } from '@/api/subscription';
import type { SubscriptionHealth } from '@/api/subscription-health';
import type { SubscriptionMetrics } from '@/api/subscription-metrics';

export function fetchTopic(
  topicName: string,
): ResponsePromise<TopicWithSchema> {
  return axios.get<TopicWithSchema>(`/topics/${topicName}`);
}

export function fetchOwner(ownerId: string): ResponsePromise<Owner> {
  return axios.get<Owner>(`/owners/sources/Service Catalog/${ownerId}`);
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
  );
}

export function activateSubscription(
  topicName: string,
  subscriptionName: string,
): ResponsePromise<void> {
  return axios.put(
    `/topics/${topicName}/subscriptions/${subscriptionName}/state`,
    State.ACTIVE,
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
  desiredState: boolean,
): ResponsePromise<void> {
  return axios.post(
    `/readiness/datacenters/${datacenter}`,
    qs.stringify({
      isReady: desiredState,
    }),
    {
      'Content-Type': 'application/json',
    } as AxiosRequestConfig,
  );
}

export function upsertTopicConstraint(
  topicName: string,
  constraint: Constraint,
): ResponsePromise<void> {
  const body: TopicConstraint = {
    topicName: topicName,
    constraint: constraint,
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
  constraint: Constraint,
): ResponsePromise<void> {
  const body: SubscriptionConstraint = {
    subscriptionName: subscriptionName,
    constraint: constraint,
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
