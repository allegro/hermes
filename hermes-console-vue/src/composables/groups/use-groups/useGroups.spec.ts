import { afterEach } from 'vitest';
import { dummyGroupNames } from '@/dummy/groups';
import { dummyTopicNames } from '@/dummy/topics';
import {
  fetchGroupNamesErrorHandler,
  fetchGroupNamesHandler,
  fetchTopicNamesErrorHandler,
  fetchTopicNamesHandler,
} from '@/mocks/handlers';
import { setupServer } from 'msw/node';
import { useGroups } from '@/composables/groups/use-groups/useGroups';
import { waitFor } from '@testing-library/vue';

describe('useGroups', () => {
  const server = setupServer(
    fetchGroupNamesHandler({ groupNames: dummyGroupNames }),
    fetchTopicNamesHandler({ topicNames: dummyTopicNames }),
  );

  afterEach(() => {
    server.resetHandlers();
  });

  it('should fetch group and topic names from Hermes backend', async () => {
    // given
    server.listen();

    // when
    const { groups, loading, error } = useGroups();

    // then
    expect(loading.value).toBeTruthy();
    expect(error.value.fetchTopicNames).toBeNull();
    expect(error.value.fetchGroupNames).toBeNull();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchGroupNames).toBeNull();
      expect(error.value.fetchTopicNames).toBeNull();
      expect(groups.value?.length).toBe(6);
      expect(groups.value?.[0].name).toBe('pl.allegro.public.admin');
      expect(groups.value?.[0].topics[0]).toContain('AdminOfferActionEvent');
      expect(groups.value?.[2].topics.length).toBe(3);
      expect(groups.value?.[3].topics.length).toBe(4);
    });
  });

  it('should set error to true on /groups endpoint failure', async () => {
    // given
    server.use(fetchGroupNamesErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { error } = useGroups();

    // then
    await waitFor(() => {
      expect(error.value.fetchGroupNames).not.toBeNull();
    });
  });

  it('should set error to true on /topics endpoint failure', async () => {
    // given
    server.use(fetchTopicNamesErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { error } = useGroups();

    // then
    await waitFor(() => {
      expect(error.value.fetchTopicNames).not.toBeNull();
    });
  });
});
