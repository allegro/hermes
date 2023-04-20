import { dummyConstraints } from '@/dummy/constraints';
import { useConstraints } from '@/composables/use-constraints/useConstraints';
import { waitFor } from '@testing-library/vue';
import axios from 'axios';
import type { Mocked } from 'vitest';

vitest.mock('axios');
const mockedAxios = axios as Mocked<typeof axios>;

describe('useConstraints', () => {
  it('should hit constraints Hermes API endpoint', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: [] });

    // when
    useConstraints();

    // then
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe('/workload-constraints');
    });
  });

  it('should fetch constraints names from Hermes backend', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyConstraints });

    // when
    const { topicConstraints, subscriptionConstraints, loading, error } =
      useConstraints();

    // then
    expect(loading.value).toBe(true);
    expect(error.value).toBe(false);

    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(false);
      expect(topicConstraints.value?.['pl.group.Topic1'].consumersNumber).toBe(
        2,
      );
      expect(
        subscriptionConstraints.value?.['pl.group.Topic$subscription2']
          .consumersNumber,
      ).toBe(8);
    });
  });

  it('should set error to true on workload endpoint failure', async () => {
    // given
    mockedAxios.get.mockRejectedValueOnce({});

    // when
    const { error } = useConstraints();

    // then
    await waitFor(() => {
      expect(error.value).toBe(true);
    });
  });
});
