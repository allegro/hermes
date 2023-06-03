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
    const { metrics, error, isLoading } = useTopicMetrics(topicName);

    // then: loading state was indicated
    expect(metrics.value).toBeUndefined();
    expect(error.value).toBeFalsy();
    expect(isLoading.value).toBeTruthy();

    // and: endpoints were called
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe(
        `/topics/${topicName}/metrics`,
      );
    });

    // and: correct data was returned
    await waitFor(() => {
      expect(metrics.value).toEqual(dummyTopicMetrics);
      expect(error.value).toBeFalsy();
      expect(isLoading.value).toBeFalsy();
    });
  });

  it('should set error to true when failed getting topic metrics', async () => {
    // given
    mockedAxios.get.mockRejectedValueOnce({});

    // when
    const { metrics, error, isLoading } = useTopicMetrics(topicName);

    // and: correct data was returned
    await waitFor(() => {
      expect(metrics.value).toBeUndefined();
      expect(error.value).toBeTruthy();
      expect(isLoading.value).toBeFalsy();
    });
  });
});
