import { dummyConsumerGroups } from '@/dummy/consumerGroups';
import { useConsumerGroups } from '@/composables/use-consumer-groups/useConsumerGroups';
import { waitFor } from '@testing-library/vue';
import axios from 'axios';
import type { Mocked } from 'vitest';

vitest.mock('axios');
const mockedAxios = axios as Mocked<typeof axios>;

describe('useConsumerGroups', () => {
  it('should hit expected Hermes API endpoint', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyConsumerGroups });

    // when
    useConsumerGroups('topic', 'subscription');

    // then
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe(
        '/topics/topic/subscriptions/subscription/consumer-groups',
      );
    });
  });

  it('should fetch consumerGroups details from Hermes API', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyConsumerGroups });

    // when
    const { consumerGroups, loading, error } = useConsumerGroups(
      'topic',
      'subscription',
    );

    // then
    expect(loading.value).toBe(true);

    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(false);
      expect(consumerGroups.value).toEqual(consumerGroups.value);
    });
  });

  it('should set error to true on consumerGroups endpoint failure', async () => {
    // given
    mockedAxios.get.mockRejectedValueOnce({});

    // when
    const { loading, error } = useConsumerGroups('topic', 'subscription');

    // then
    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(true);
    });
  });
});
