import { dummyInconsistentTopics } from '@/dummy/inconsistentTopics';
import { useInconsistentTopics } from '@/composables/use-inconsistent-topics/useInconsistentTopics';
import { waitFor } from '@testing-library/vue';
import axios from 'axios';
import type { Mocked } from 'vitest';

vitest.mock('axios');
const mockedAxios = axios as Mocked<typeof axios>;

describe('useInconsistentTopics', () => {
  it('should hit expected Hermes API endpoint', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyInconsistentTopics });

    // when
    useInconsistentTopics();

    // then
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe(
        '/consistency/inconsistencies/topics',
      );
    });
  });

  it('should fetch topics consistency details from Hermes API', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyInconsistentTopics });

    // when
    const { topics, loading, error } = useInconsistentTopics();

    // then
    expect(loading.value).toBe(true);

    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(false);
      expect(topics.value).toEqual(dummyInconsistentTopics);
    });
  });

  it('should set error to true on topics consistency endpoint failure', async () => {
    // given
    mockedAxios.get.mockRejectedValueOnce({});

    // when
    const { loading, error } = useInconsistentTopics();

    // then
    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(true);
    });
  });
});
