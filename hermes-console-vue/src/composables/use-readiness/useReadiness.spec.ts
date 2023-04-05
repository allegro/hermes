import { dummyDatacentersReadiness } from '@/dummy/readiness';
import { useReadiness } from '@/composables/use-readiness/useReadiness';
import { waitFor } from '@testing-library/vue';
import axios from 'axios';
import type { Mocked } from 'vitest';

vitest.mock('axios');
const mockedAxios = axios as Mocked<typeof axios>;

describe('useReadiness', () => {
  it('should hit expected Hermes API endpoint', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyDatacentersReadiness });

    // when
    useReadiness();

    // then
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe('/readiness/datacenters');
    });
  });

  it('should fetch readiness details from Hermes API', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyDatacentersReadiness });

    // when
    const { datacentersReadiness, loading, error } = useReadiness();

    // then
    expect(loading.value).toBe(true);

    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(false);
      expect(datacentersReadiness.value).toEqual(dummyDatacentersReadiness);
    });
  });

  it('should set error to true on datacenters readiness endpoint failure', async () => {
    // given
    mockedAxios.get.mockRejectedValueOnce({});

    // when
    const { loading, error } = useReadiness();

    // then
    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(true);
    });
  });
});
