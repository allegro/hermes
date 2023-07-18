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
import type {
  MessagePreview,
  TopicMetrics,
  TopicWithSchema,
} from '@/api/topic';
import type { Owner } from '@/api/owner';
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
