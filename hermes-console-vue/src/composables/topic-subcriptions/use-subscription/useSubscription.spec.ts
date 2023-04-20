import {
  dummySubscription,
  dummySubscriptionHealth,
  dummySubscriptionMetrics,
  dummyUndeliveredMessage,
  dummyUndeliveredMessages,
} from '@/dummy/subscription';
import { useSubscription } from '@/composables/topic-subcriptions/use-subscription/useSubscription';
import { waitFor } from '@testing-library/vue';
import axios from 'axios';
import type { Mocked } from 'vitest';
import { beforeEach } from 'vitest';

vitest.mock('axios');
const mockedAxios = axios as Mocked<typeof axios>;

describe('useSubscription', () => {
  beforeEach(() => {
    vitest.resetAllMocks();
  });

  it('should hit expected Hermes API endpoints', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscription });
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscriptionMetrics });
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscriptionHealth });
    mockedAxios.get.mockResolvedValueOnce({ data: dummyUndeliveredMessages });
    mockedAxios.get.mockResolvedValueOnce({ data: dummyUndeliveredMessage });

    // when
    useSubscription('topic', 'subscription');

    // then
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe(
        '/topics/topic/subscriptions/subscription',
      );
      expect(mockedAxios.get.mock.calls[1][0]).toBe(
        '/topics/topic/subscriptions/subscription/metrics',
      );
      expect(mockedAxios.get.mock.calls[2][0]).toBe(
        '/topics/topic/subscriptions/subscription/health',
      );
      expect(mockedAxios.get.mock.calls[3][0]).toBe(
        '/topics/topic/subscriptions/subscription/undelivered',
      );
      expect(mockedAxios.get.mock.calls[4][0]).toBe(
        '/topics/topic/subscriptions/subscription/undelivered/last',
      );
    });
  });

  it('should fetch subscription details from Hermes API', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscription });
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscriptionMetrics });
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscriptionHealth });
    mockedAxios.get.mockResolvedValueOnce({ data: dummyUndeliveredMessages });
    mockedAxios.get.mockResolvedValueOnce({ data: dummyUndeliveredMessage });

    // when
    const {
      subscription,
      subscriptionMetrics,
      subscriptionHealth,
      loading,
      error,
    } = useSubscription('topic', 'subscription');

    // then
    expect(loading.value).toBe(true);

    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(false);
      expect(subscription.value).toEqual(dummySubscription);
      expect(subscriptionMetrics.value).toEqual(dummySubscriptionMetrics);
      expect(subscriptionHealth.value).toEqual(dummySubscriptionHealth);
    });
  });

  it('should set error to true on subscription endpoint failure', async () => {
    // given
    mockedAxios.get.mockRejectedValueOnce({});
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscriptionMetrics });
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscriptionHealth });
    mockedAxios.get.mockResolvedValueOnce({ data: dummyUndeliveredMessages });
    mockedAxios.get.mockResolvedValueOnce({ data: dummyUndeliveredMessage });

    // when
    const { loading, error } = useSubscription('topic', 'subscription');

    // then
    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(true);
    });
  });

  it('should set error to true on subscription metrics endpoint failure', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscription });
    mockedAxios.get.mockRejectedValueOnce({});
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscriptionHealth });
    mockedAxios.get.mockResolvedValueOnce({ data: dummyUndeliveredMessages });
    mockedAxios.get.mockResolvedValueOnce({ data: dummyUndeliveredMessage });

    // when
    const { loading, error } = useSubscription('topic', 'subscription');

    // then
    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(true);
    });
  });

  it('should set error to true on subscription health endpoint failure', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscription });
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscriptionMetrics });
    mockedAxios.get.mockRejectedValueOnce({});
    mockedAxios.get.mockResolvedValueOnce({ data: dummyUndeliveredMessages });
    mockedAxios.get.mockResolvedValueOnce({ data: dummyUndeliveredMessage });

    // when
    const { loading, error } = useSubscription('topic', 'subscription');

    // then
    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(true);
    });
  });

  it('should set last undelivered message to `null` on backend HTTP 404', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscription });
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscriptionMetrics });
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscriptionHealth });
    mockedAxios.get.mockResolvedValueOnce({ data: dummyUndeliveredMessages });
    mockedAxios.get.mockRejectedValueOnce({});

    // when
    const { subscriptionLastUndeliveredMessage, loading, error } =
      useSubscription('topic', 'subscription');

    // then
    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(false);
      expect(subscriptionLastUndeliveredMessage.value).toBe(null);
    });
  });

  it('should ignore undelivered endpoint failure and set empty list', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscription });
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscriptionMetrics });
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscriptionHealth });
    mockedAxios.get.mockRejectedValueOnce({});
    mockedAxios.get.mockResolvedValueOnce({ data: dummyUndeliveredMessage });

    // when
    const { subscriptionUndeliveredMessages, loading, error } = useSubscription(
      'topic',
      'subscription',
    );

    // then
    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(false);
      expect(subscriptionUndeliveredMessages.value).toEqual([]);
    });
  });
});
