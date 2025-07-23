import { afterEach, expect } from 'vitest';
import {
  createRetransmissionHandler,
  fetchSubscriptionErrorHandler,
  fetchSubscriptionHealthErrorHandler,
  fetchSubscriptionLastUndeliveredMessageErrorHandler,
  fetchSubscriptionMetricsErrorHandler,
  fetchSubscriptionUndeliveredMessagesErrorHandler,
  removeSubscriptionErrorHandler,
  removeSubscriptionHandler,
  subscriptionStateErrorHandler,
  subscriptionStateHandler,
  successfulSubscriptionHandlers,
} from '@/mocks/handlers';
import { createTestingPinia } from '@pinia/testing';
import {
  dummySubscription,
  dummySubscriptionHealth,
  dummySubscriptionMetrics,
} from '@/dummy/subscription';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useSubscription } from '@/composables/subscription/use-subscription/useSubscription';
import { waitFor } from '@testing-library/vue';
import type { UseSubscriptionsErrors } from '@/composables/subscription/use-subscription/useSubscription';

describe('useSubscription', () => {
  const server = setupServer(...successfulSubscriptionHandlers);

  const pinia = createTestingPinia({
    fakeApp: true,
  });

  beforeEach(() => {
    setActivePinia(pinia);
  });

  afterEach(() => {
    server.resetHandlers();
  });

  it('should fetch subscription details from Hermes API', async () => {
    // given
    server.listen();

    // when
    const {
      subscription,
      subscriptionMetrics,
      subscriptionHealth,
      loading,
      error,
    } = useSubscription(dummySubscription.topicName, dummySubscription.name);

    // then
    expect(loading.value).toBeTruthy();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expectNoErrors(error.value);
      expect(subscription.value).toEqual(dummySubscription);
      expect(subscriptionMetrics.value).toEqual(dummySubscriptionMetrics);
      expect(subscriptionHealth.value).toEqual(dummySubscriptionHealth);
    });
  });

  it('should set error to true on subscription endpoint failure', async () => {
    // given
    server.use(fetchSubscriptionErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { loading, error } = useSubscription('topic', 'subscription');

    // then
    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchSubscription).not.toBeNull();
    });
  });

  it('should set error to true on subscription metrics endpoint failure', async () => {
    // given
    server.use(fetchSubscriptionMetricsErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { loading, error } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // then
    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchSubscriptionMetrics).not.toBeNull();
    });
  });

  it('should set error to true on subscription health endpoint failure', async () => {
    // given
    server.use(fetchSubscriptionHealthErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { loading, error } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // then
    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchSubscriptionHealth).not.toBeNull();
    });
  });

  it('should set last undelivered message to `null` on backend HTTP 404', async () => {
    // given
    server.use(
      fetchSubscriptionLastUndeliveredMessageErrorHandler({ errorCode: 404 }),
    );
    server.listen();

    // when
    const { subscriptionLastUndeliveredMessage, loading, error } =
      useSubscription(dummySubscription.topicName, dummySubscription.name);

    // then
    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(
        error.value.fetchSubscriptionLastUndeliveredMessage,
      ).not.toBeNull();
      expect(subscriptionLastUndeliveredMessage.value).toBeNull();
    });
  });

  it('should ignore undelivered endpoint failure and set empty list', async () => {
    // given
    server.use(
      fetchSubscriptionUndeliveredMessagesErrorHandler({ errorCode: 404 }),
    );
    server.listen();

    // when
    const { subscriptionUndeliveredMessages, loading, error } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // then
    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchSubscriptionUndeliveredMessages).not.toBeNull();
      expect(subscriptionUndeliveredMessages.value).toEqual([]);
    });
  });

  it('should show message that removing subscription was successful', async () => {
    // given
    server.use(
      removeSubscriptionHandler({
        topic: dummySubscription.topicName,
        subscription: dummySubscription.name,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { removeSubscription } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // when
    await removeSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        text: 'notifications.subscription.delete.success',
      });
    });
  });

  it('should show message that removing subscription was unsuccessful', async () => {
    // given
    server.use(
      removeSubscriptionErrorHandler({
        topic: dummySubscription.topicName,
        subscription: dummySubscription.name,
        errorCode: 500,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { removeSubscription } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // when
    await removeSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.subscription.delete.failure',
      });
    });
  });

  it('should show message that suspending subscription was successful', async () => {
    // given
    server.use(
      subscriptionStateHandler({
        topic: dummySubscription.topicName,
        subscription: dummySubscription.name,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { suspendSubscription } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // when
    await suspendSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        text: 'notifications.subscription.suspend.success',
      });
    });
  });

  it('should show message that suspending subscription was unsuccessful', async () => {
    // given
    server.use(
      subscriptionStateErrorHandler({
        topic: dummySubscription.topicName,
        subscription: dummySubscription.name,
        errorCode: 500,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { suspendSubscription } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // when
    await suspendSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.subscription.suspend.failure',
      });
    });
  });

  it('should show message that activating subscription was successful', async () => {
    // given
    server.use(
      subscriptionStateHandler({
        topic: dummySubscription.topicName,
        subscription: dummySubscription.name,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { activateSubscription } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // when
    await activateSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        text: 'notifications.subscription.activate.success',
      });
    });
  });

  it('should show message that activating subscription was unsuccessful', async () => {
    // given
    server.use(
      subscriptionStateErrorHandler({
        topic: dummySubscription.topicName,
        subscription: dummySubscription.name,
        errorCode: 500,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { activateSubscription } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // when
    await activateSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.subscription.activate.failure',
      });
    });
  });

  it('should show message that retransmission was successful', async () => {
    // given
    server.use(
      createRetransmissionHandler({
        topicName: dummySubscription.topicName,
        subscriptionName: dummySubscription.name,
        statusCode: 200,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { retransmitMessages } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // when
    await retransmitMessages(new Date().toISOString());

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        title: 'notifications.subscription.retransmit.success',
      });
    });
  });

  it('should show message that retransmission was unsuccessful', async () => {
    // given
    server.use(
      createRetransmissionHandler({
        topicName: dummySubscription.topicName,
        subscriptionName: dummySubscription.name,
        statusCode: 500,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { retransmitMessages } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // when
    await retransmitMessages(new Date().toISOString());

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.subscription.retransmit.failure',
      });
    });
  });

  [200, 500].forEach((statusCode) => {
    it(`should correctly manage the state of retransmission with status code ${statusCode}`, async () => {
      // given
      server.use(
        createRetransmissionHandler({
          topicName: dummySubscription.topicName,
          subscriptionName: dummySubscription.name,
          statusCode,
          delayMs: 100,
        }),
      );
      server.listen();

      const { retransmitMessages, retransmitting } = useSubscription(
        dummySubscription.topicName,
        dummySubscription.name,
      );

      expect(retransmitting.value).toBeFalsy();

      // when
      retransmitMessages(new Date().toISOString());

      // then
      await waitFor(() => {
        expect(retransmitting.value).toBeTruthy();
      });
      await waitFor(() => {
        expect(retransmitting.value).toBeFalsy();
      });
    });
  });

  it('should show message that skipping all messages was successful', async () => {
    // given
    server.use(
      createRetransmissionHandler({
        topicName: dummySubscription.topicName,
        subscriptionName: dummySubscription.name,
        statusCode: 200,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { skipAllMessages } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // when
    await skipAllMessages();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        title: 'notifications.subscription.skipAllMessages.success',
      });
    });
  });

  it('should show message that skipping all messages was unsuccessful', async () => {
    // given
    server.use(
      createRetransmissionHandler({
        topicName: dummySubscription.topicName,
        subscriptionName: dummySubscription.name,
        statusCode: 500,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { skipAllMessages } = useSubscription(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // when
    await skipAllMessages();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.subscription.skipAllMessages.failure',
      });
    });
  });

  [200, 500].forEach((statusCode) => {
    it(`should correctly manage the state of skipping all messages with status code ${statusCode}`, async () => {
      // given
      server.use(
        createRetransmissionHandler({
          topicName: dummySubscription.topicName,
          subscriptionName: dummySubscription.name,
          statusCode,
          delayMs: 100,
        }),
      );
      server.listen();

      const { skipAllMessages, skippingAllMessages } = useSubscription(
        dummySubscription.topicName,
        dummySubscription.name,
      );

      expect(skippingAllMessages.value).toBeFalsy();

      // when
      skipAllMessages();

      // then
      await waitFor(() => {
        expect(skippingAllMessages.value).toBeTruthy();
      });
      await waitFor(() => {
        expect(skippingAllMessages.value).toBeFalsy();
      });
    });
  });
});

function expectErrors(
  errors: UseSubscriptionsErrors,
  {
    fetchSubscription = false,
    fetchOwner = false,
    fetchSubscriptionMetrics = false,
    fetchSubscriptionHealth = false,
    fetchSubscriptionUndeliveredMessages = false,
    fetchSubscriptionLastUndeliveredMessage = false,
  },
) {
  (fetchSubscription && expect(errors.fetchSubscription).not.toBeNull()) ||
    expect(errors.fetchSubscription).toBeNull();
  (fetchOwner && expect(errors.fetchOwner).not.toBeNull()) ||
    expect(errors.fetchOwner).toBeNull();
  (fetchSubscriptionMetrics &&
    expect(errors.fetchSubscriptionMetrics).not.toBeNull()) ||
    expect(errors.fetchSubscriptionMetrics).toBeNull();
  (fetchSubscriptionHealth &&
    expect(errors.fetchSubscriptionHealth).not.toBeNull()) ||
    expect(errors.fetchSubscriptionHealth).toBeNull();
  (fetchSubscriptionUndeliveredMessages &&
    expect(errors.fetchSubscriptionUndeliveredMessages).not.toBeNull()) ||
    expect(errors.fetchSubscriptionUndeliveredMessages).toBeNull();
  (fetchSubscriptionLastUndeliveredMessage &&
    expect(errors.fetchSubscriptionLastUndeliveredMessage).not.toBeNull()) ||
    expect(errors.fetchSubscriptionLastUndeliveredMessage).toBeNull();
}

function expectNoErrors(errors: UseSubscriptionsErrors) {
  expectErrors(errors, {});
}
