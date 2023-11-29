import { afterEach } from 'vitest';
import { fetchStatsErrorHandler, fetchStatsHandler } from '@/mocks/handlers';
import { setupServer } from 'msw/node';
import { statsResponse } from '@/dummy/stats';
import { useStats } from '@/composables/stats/use-stats/useStats';
import { waitFor } from '@testing-library/vue';

describe('useStats', () => {
  const server = setupServer(fetchStatsHandler({ stats: statsResponse }));

  afterEach(() => {
    server.resetHandlers();
  });

  it('should fetch stats details from Hermes API', async () => {
    // given
    server.listen();

    // when
    const { stats, loading, error } = useStats();

    // then
    expect(loading.value).toBeTruthy();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchError).toBeNull();
      const {
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
      } = stats.value!!;

      expect(topicCount).toEqual(statsResponse.topicStats.topicCount);
      expect(ackAllTopicCount).toEqual(
        statsResponse.topicStats.ackAllTopicCount,
      );
      expect(ackAllTopicShare).toBeCloseTo(
        share(
          statsResponse.topicStats.ackAllTopicCount,
          statsResponse.topicStats.topicCount,
        ),
        0.001,
      );
      expect(avroTopicCount).toEqual(statsResponse.topicStats.avroTopicCount);
      expect(avroTopicShare).toBeCloseTo(
        share(
          statsResponse.topicStats.avroTopicCount,
          statsResponse.topicStats.topicCount,
        ),
        0.001,
      );
      expect(trackingEnabledTopicCount).toEqual(
        statsResponse.topicStats.trackingEnabledTopicCount,
      );
      expect(trackingEnabledTopicShare).toBeCloseTo(
        share(
          statsResponse.topicStats.trackingEnabledTopicCount,
          statsResponse.topicStats.topicCount,
        ),
        0.001,
      );
      expect(subscriptionCount).toEqual(
        statsResponse.subscriptionStats.subscriptionCount,
      );
      expect(avroSubscriptionCount).toEqual(
        statsResponse.subscriptionStats.avroSubscriptionCount,
      );
      expect(avroSubscriptionShare).toBeCloseTo(
        share(
          statsResponse.subscriptionStats.avroSubscriptionCount,
          statsResponse.subscriptionStats.subscriptionCount,
        ),
        0.001,
      );
      expect(trackingEnabledSubscriptionCount).toEqual(
        statsResponse.subscriptionStats.trackingEnabledSubscriptionCount,
      );
      expect(trackingEnabledSubscriptionShare).toBeCloseTo(
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
    server.use(fetchStatsErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { loading, error } = useStats();

    // then
    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value.fetchError).not.toBeNull();
    });
  });
});

function share(numerator: number, denominator: number): number {
  return (numerator / denominator) * 100;
}
