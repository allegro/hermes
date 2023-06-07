import { beforeEach, describe, expect } from 'vitest';
import { useFetchedData } from '@/composables/use-fetched-data/useFetchedData';
import { waitFor } from '@testing-library/vue';
import axios from 'axios';
import type { Mocked } from 'vitest';

vitest.mock('axios');
const mockedAxios = axios as Mocked<typeof axios>;

describe('useFetchedData', () => {
  beforeEach(() => {
    vitest.resetAllMocks();
  });

  it('should fetch data', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: { text: 'Sample text' } });
    const request = () => axios.get('http://localhost/mock');

    // when
    const { data, error, isLoading } = useFetchedData({ request });

    // then: loading state was indicated
    expect(data.value).toBeUndefined();
    expect(error.value).toBeFalsy();
    expect(isLoading.value).toBeTruthy();

    // and: endpoints were called
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe(`http://localhost/mock`);
    });

    // and: correct data was returned
    await waitFor(() => {
      expect(data.value).toEqual({ text: 'Sample text' });
      expect(error.value).toBeFalsy();
      expect(isLoading.value).toBeFalsy();
    });
  });

  it('should set error to true when failed getting topic metrics', async () => {
    // given
    mockedAxios.get.mockRejectedValueOnce({});
    const request = () => axios.get('http://localhost/mock');

    // when
    const { data, error, isLoading } = useFetchedData({ request });

    // and: correct data was returned
    await waitFor(() => {
      expect(data.value).toBeUndefined();
      expect(error.value).toBeTruthy();
      expect(isLoading.value).toBeFalsy();
    });
  });
});
