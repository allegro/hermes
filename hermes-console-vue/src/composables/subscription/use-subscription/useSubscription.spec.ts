import { afterEach } from 'vitest';
import {
  dummySubscription,
  dummySubscriptionHealth,
  dummySubscriptionMetrics,
} from '@/dummy/subscription';
import { expect } from 'vitest';
import {
  fetchSubscriptionErrorHandler,
  fetchSubscriptionHealthErrorHandler,
  fetchSubscriptionLastUndeliveredMessageErrorHandler,
  fetchSubscriptionMetricsErrorHandler,
  fetchSubscriptionUndeliveredMessagesErrorHandler,
  successfulSubscriptionHandlers,
} from '@/mocks/handlers';
import { setupServer } from 'msw/node';
import { useSubscription } from '@/composables/subscription/use-subscription/useSubscription';
import { waitFor } from '@testing-library/vue';
import type { UseSubscriptionsErrors } from '@/composables/subscription/use-subscription/useSubscription';

describe('useSubscription', () => {
  const server = setupServer(...successfulSubscriptionHandlers);

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
