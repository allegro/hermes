import { afterEach, describe, expect } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import {
  dummyOwner,
  dummyTopic,
  dummyTopicMessagesPreview,
  dummyTopicMetrics,
} from '@/dummy/topic';
import {
  dummySubscription,
  secondDummySubscription,
} from '@/dummy/subscription';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import {
  fetchOwnerErrorHandler,
  fetchTopicClientsErrorHandler,
  fetchTopicErrorHandler,
  fetchTopicMessagesPreviewErrorHandler,
  fetchTopicMetricsErrorHandler,
  fetchTopicSubscriptionDetailsErrorHandler,
  fetchTopicSubscriptionsErrorHandler,
  removeTopicErrorHandler,
  removeTopicHandler,
  successfulTopicHandlers,
} from '@/mocks/handlers';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useTopic } from '@/composables/topic/use-topic/useTopic';
import { waitFor } from '@testing-library/vue';
import type { UseTopicErrors } from '@/composables/topic/use-topic/useTopic';

describe('useTopic', () => {
  const server = setupServer(...successfulTopicHandlers);

  const topicName = dummyTopic.name;
  const topicOwner = dummyOwner.id;

  const pinia = createTestingPinia({
    fakeApp: true,
  });

  beforeEach(() => {
    setActivePinia(pinia);
  });

  afterEach(() => {
    server.resetHandlers();
  });

  it('should fetch topic', async () => {
    // given
    server.listen();

    // when
    const { topic, owner, metrics, messages, subscriptions, loading, error } =
      useTopic(topicName);

    // and: correct data was returned
    await waitFor(() => {
      expect(topic.value).toEqual(dummyTopic);
      expect(owner.value).toEqual(dummyOwner);
      expect(metrics.value).toEqual(dummyTopicMetrics);
      expect(messages.value).toEqual(dummyTopicMessagesPreview);
      expect(subscriptions.value).toEqual([
        dummySubscription,
        secondDummySubscription,
      ]);
    });

    // and: correct loading and error states were indicated
    expect(loading.value).toBeFalsy();
    expectNoErrors(error.value);
  });

  const expectedDataForErrorTest = {
    expectedTopic: dummyTopic,
    expectedOwner: dummyOwner,
    expectedMessages: dummyTopicMessagesPreview,
    expectedMetrics: dummyTopicMetrics,
    expectedSubscriptions: [dummySubscription, secondDummySubscription],
  };

  it.each([
    {
      mockHandler: fetchTopicErrorHandler({ topicName }),
      expectedErrors: { fetchTopic: true },
      expectedTopic: undefined,
      expectedOwner: undefined,
      expectedMessages: undefined,
      expectedMetrics: undefined,
      expectedSubscriptions: undefined,
    },
    {
      mockHandler: fetchOwnerErrorHandler({ owner: topicOwner }),
      expectedErrors: { fetchOwner: true },
      ...expectedDataForErrorTest,
      expectedOwner: undefined,
    },
    {
      mockHandler: fetchTopicMessagesPreviewErrorHandler({ topicName }),
      expectedErrors: { fetchTopicMessagesPreview: true },
      ...expectedDataForErrorTest,
      expectedMessages: undefined,
    },
    {
      mockHandler: fetchTopicMetricsErrorHandler({ topicName }),
      expectedErrors: { fetchTopicMetrics: true },
      ...expectedDataForErrorTest,
      expectedMetrics: undefined,
    },
    {
      mockHandler: fetchTopicSubscriptionsErrorHandler({ topicName }),
      expectedErrors: { fetchSubscriptions: true },
      ...expectedDataForErrorTest,
      expectedSubscriptions: undefined,
    },
    {
      mockHandler: fetchTopicSubscriptionDetailsErrorHandler({
        topicName,
        subscriptionName: dummySubscription.name,
      }),
      expectedErrors: { fetchSubscriptions: true },
      ...expectedDataForErrorTest,
      expectedSubscriptions: [secondDummySubscription],
    },
    {
      mockHandler: fetchTopicSubscriptionDetailsErrorHandler({
        topicName,
        subscriptionName: secondDummySubscription.name,
      }),
      expectedErrors: { fetchSubscriptions: true },
      ...expectedDataForErrorTest,
      expectedSubscriptions: [dummySubscription],
    },
  ])(
    'should indicate appropriate error',
    async ({
      mockHandler,
      expectedErrors,
      expectedTopic,
      expectedOwner,
      expectedMessages,
      expectedMetrics,
      expectedSubscriptions,
    }) => {
      // given
      server.use(mockHandler);
      server.listen();

      // when
      const { topic, owner, metrics, messages, subscriptions, loading, error } =
        useTopic(topicName);

      // then
      await waitFor(() => {
        expect(topic.value).toEqual(expectedTopic);
        expect(owner.value).toEqual(expectedOwner);
        expect(messages.value).toEqual(expectedMessages);
        expect(metrics.value).toEqual(expectedMetrics);
        expect(subscriptions.value).toEqual(expectedSubscriptions);
        expect(loading.value).toBeFalsy();
        expectErrors(error.value, expectedErrors);
      });
    },
  );

  it('should show message that removing topic was successful', async () => {
    // given
    server.use(removeTopicHandler({ topic: dummyTopic.name }));
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { removeTopic } = useTopic(dummyTopic.name);

    // when
    await removeTopic();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        text: 'notifications.topic.delete.success',
      });
    });
  });

  it('should show message that removing topic was unsuccessful', async () => {
    // given
    server.use(
      removeTopicErrorHandler({ topic: dummyTopic.name, errorCode: 500 }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { removeTopic } = useTopic(dummyTopic.name);

    // when
    await removeTopic();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.topic.delete.failure',
      });
    });
  });

  it('should show message that fetching clients was unsuccessful', async () => {
    // given
    server.use(fetchTopicClientsErrorHandler({ topicName: dummyTopic.name }));
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { fetchTopicClients } = useTopic(topicName);

    // when
    const clients = await fetchTopicClients();

    // then
    expect(clients).toBeNull();

    expectNotificationDispatched(notificationStore, {
      type: 'error',
      title: 'notifications.topic.clients.fetch.failure',
    });
  });
});

function expectErrors(
  errors: UseTopicErrors,
  {
    fetchTopic = false,
    fetchOwner = false,
    fetchTopicMessagesPreview = false,
    fetchTopicMetrics = false,
    fetchSubscriptions = false,
  },
) {
  (fetchTopic && expect(errors.fetchTopic).not.toBeNull()) ||
    expect(errors.fetchTopic).toBeNull();
  (fetchOwner && expect(errors.fetchOwner).not.toBeNull()) ||
    expect(errors.fetchOwner).toBeNull();
  (fetchTopicMessagesPreview &&
    expect(errors.fetchTopicMessagesPreview).not.toBeNull()) ||
    expect(errors.fetchTopicMessagesPreview).toBeNull();
  (fetchTopicMetrics && expect(errors.fetchTopicMetrics).not.toBeNull()) ||
    expect(errors.fetchTopicMetrics).toBeNull();
  (fetchSubscriptions && expect(errors.fetchSubscriptions).not.toBeNull()) ||
    expect(errors.fetchSubscriptions).toBeNull();
}

function expectNoErrors(errors: UseTopicErrors) {
  expectErrors(errors, {});
}
