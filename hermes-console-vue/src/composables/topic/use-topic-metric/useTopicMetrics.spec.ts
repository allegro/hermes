import { beforeEach, describe, expect } from 'vitest';
import { dummyTopicMetrics } from '@/dummy/topic';
import { useTopicMetrics } from '@/composables/topic/use-topic-metric/useTopicMetrics';
import { waitFor } from '@testing-library/vue';
import axios from 'axios';
import type { Mocked } from 'vitest';

vitest.mock('axios');
const mockedAxios = axios as Mocked<typeof axios>;

describe('useTopicMetrics', () => {
  const topicName = 'pl.allegro.public.group.DummyEvent';

  beforeEach(() => {
    vitest.resetAllMocks();
  });

  it('should fetch topic metrics', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyTopicMetrics });

    // when
    const { data, error, isLoading } = useTopicMetrics(topicName);

    // then
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe(
        `/topics/${topicName}/metrics`,
      );
    });

    // and: correct data was returned
    await waitFor(() => {
      expect(data.value).toEqual(dummyTopicMetrics);
      expect(error.value).toBeFalsy();
      expect(isLoading.value).toBeFalsy();
    });
  });
});
