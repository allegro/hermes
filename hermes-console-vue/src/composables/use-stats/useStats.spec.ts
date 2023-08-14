import { expect } from 'vitest';
import { statsResponse } from '@/dummy/stats';
import { useStats } from '@/composables/use-stats/useStats';
import { waitFor } from '@testing-library/vue';
import axios from 'axios';
import type { Mocked } from 'vitest';

vitest.mock('axios');
const mockedAxios = axios as Mocked<typeof axios>;

describe('useStats', () => {
  it('should hit expected Hermes API endpoint', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: statsResponse });

    // when
    useStats();

    // then
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe('/stats');
    });
  });

  it('should fetch stats details from Hermes API', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: statsResponse });

    // when
    const {
      error,
      loading,
      topicCount,
      ackAllTopicCount,
      ackAllTopicShare,
      trackingEnabledTopicCount,
      trackingEnabledTopicShare,
      avroTopicCount,
      avroTopicShare,
      subscriptionCount,
      trackingEnabledSubscriptionCount,
      trackingEnabledSubscriptionShare,
      avroSubscriptionCount,
      avroSubscriptionShare,
    } = useStats();

    // then
    expect(loading.value).toBe(true);

    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(false);
      expect(topicCount.value).toEqual(statsResponse.topicStats.topicCount);
      expect(ackAllTopicCount.value).toEqual(
        statsResponse.topicStats.ackAllTopicCount,
      );
      expect(ackAllTopicShare.value).toBeCloseTo(
        share(
          statsResponse.topicStats.ackAllTopicCount,
          statsResponse.topicStats.topicCount,
        ),
        0.001,
      );
      expect(avroTopicCount.value).toEqual(
        statsResponse.topicStats.avroTopicCount,
      );
      expect(avroTopicShare.value).toBeCloseTo(
        share(
          statsResponse.topicStats.avroTopicCount,
          statsResponse.topicStats.topicCount,
        ),
        0.001,
      );
      expect(trackingEnabledTopicCount.value).toEqual(
        statsResponse.topicStats.trackingEnabledTopicCount,
      );
      expect(trackingEnabledTopicShare.value).toBeCloseTo(
        share(
          statsResponse.topicStats.trackingEnabledTopicCount,
          statsResponse.topicStats.topicCount,
        ),
        0.001,
      );
      expect(subscriptionCount.value).toEqual(
        statsResponse.subscriptionStats.subscriptionCount,
      );
      expect(avroSubscriptionCount.value).toEqual(
        statsResponse.subscriptionStats.avroSubscriptionCount,
      );
      expect(avroSubscriptionShare.value).toBeCloseTo(
        share(
          statsResponse.subscriptionStats.avroSubscriptionCount,
          statsResponse.subscriptionStats.subscriptionCount,
        ),
        0.001,
      );
      expect(trackingEnabledSubscriptionCount.value).toEqual(
        statsResponse.subscriptionStats.trackingEnabledSubscriptionCount,
      );
      expect(trackingEnabledSubscriptionShare.value).toBeCloseTo(
        share(
          statsResponse.subscriptionStats.trackingEnabledSubscriptionCount,
          statsResponse.subscriptionStats.subscriptionCount,
        ),
        0.001,
      );
    });
  });

  it('should set error to true on stats endpoint failure', async () => {
    // given
    mockedAxios.get.mockRejectedValueOnce({});

    // when
    const { loading, error } = useStats();

    // then
    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(true);
    });
  });
});

function share(numerator: number, denominator: number): number {
  return (numerator / denominator) * 100;
}
