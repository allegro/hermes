import { dummyGroupNames } from '@/dummy/groups';
import { dummyTopicNames } from '@/dummy/topics';
import { useGroups } from '@/composables/use-groups/useGroups';
import { waitFor } from '@testing-library/vue';
import axios from 'axios';
import type { Mocked } from 'vitest';

vitest.mock('axios');
const mockedAxios = axios as Mocked<typeof axios>;

describe('useGroups', () => {
  it('should hit groups and topics Hermes API endpoints', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: [] });
    mockedAxios.get.mockResolvedValueOnce({ data: [] });

    // when
    useGroups();

    // then
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe('/groups');
      expect(mockedAxios.get.mock.calls[1][0]).toBe('/topics');
    });
  });

  it('should fetch group and topic names from Hermes backend', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyGroupNames });
    mockedAxios.get.mockResolvedValueOnce({ data: dummyTopicNames });

    // when
    const { groups, loading, error } = useGroups();

    // then
    expect(loading.value).toBe(true);
    expect(error.value).toBe(false);

    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value).toBe(false);
      expect(groups.value?.length).toBe(6);
      expect(groups.value?.[0].name).toBe('pl.allegro.public.admin');
      expect(groups.value?.[0].topics[0]).toContain('AdminOfferActionEvent');
      expect(groups.value?.[2].topics.length).toBe(3);
      expect(groups.value?.[3].topics.length).toBe(4);
    });
  });

  it('should set error to true on /groups endpoint failure', async () => {
    // given
    mockedAxios.get.mockRejectedValueOnce({});
    mockedAxios.get.mockResolvedValueOnce({ data: dummyTopicNames });

    // when
    const { error } = useGroups();

    // then
    await waitFor(() => {
      expect(error.value).toBe(true);
    });
  });

  it('should set error to true on /topics endpoint failure', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyGroupNames });
    mockedAxios.get.mockRejectedValueOnce({});

    // when
    const { error } = useGroups();

    // then
    await waitFor(() => {
      expect(error.value).toBe(true);
    });
  });
});
