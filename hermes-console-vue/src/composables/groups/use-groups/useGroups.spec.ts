import { afterEach } from 'vitest';
import {
  createGroupErrorHandler,
  createGroupHandler,
  fetchGroupNamesErrorHandler,
  fetchGroupNamesHandler,
  fetchTopicNamesErrorHandler,
  fetchTopicNamesHandler,
  removeGroupErrorHandler,
  removeGroupHandler,
} from '@/mocks/handlers';
import { createTestingPinia } from '@pinia/testing';
import { dummyGroupNames } from '@/dummy/groups';
import { dummyTopicNames } from '@/dummy/topics';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useGroups } from '@/composables/groups/use-groups/useGroups';
import { waitFor } from '@testing-library/vue';

describe('useGroups', () => {
  const server = setupServer(
    fetchGroupNamesHandler({ groupNames: dummyGroupNames }),
    fetchTopicNamesHandler({ topicNames: dummyTopicNames }),
  );

  const pinia = createTestingPinia({
    fakeApp: true,
  });

  beforeEach(() => {
    setActivePinia(pinia);
  });

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

  it('should show message that removing group was successful', async () => {
    // given
    server.use(removeGroupHandler({ group: dummyGroupNames[0] }));
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { removeGroup } = useGroups();

    // when
    await removeGroup(dummyGroupNames[0]);

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        text: 'notifications.group.delete.success',
      });
    });
  });

  it('should show message that removing group was unsuccessful', async () => {
    // given
    server.use(
      removeGroupErrorHandler({ group: dummyGroupNames[0], errorCode: 500 }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { removeGroup } = useGroups();

    // when
    await removeGroup(dummyGroupNames[0]);

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.group.delete.failure',
      });
    });
  });

  it('should dispatch notification on successful group create', async () => {
    // given
    server.use(createGroupHandler({ group: { groupName: 'group' } }));
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { createGroup } = useGroups();

    // when
    await createGroup('group');

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        title: 'notifications.group.create.success',
      });
    });
  });

  it('should dispatch notification on unsuccessful group create', async () => {
    // given
    server.use(createGroupErrorHandler({}));
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { createGroup } = useGroups();

    // when
    await createGroup('group');

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.group.create.failure',
      });
    });
  });
});
