import { afterEach } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import { dummyRoles } from '@/dummy/roles';
import { fetchRolesErrorHandler, fetchRolesHandler } from '@/mocks/handlers';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import { useRoles } from '@/composables/roles/use-roles/useRoles';
import { waitFor } from '@testing-library/vue';

describe('useRoles', () => {
  const topicName = 'pl.allegro.dummyTopic';
  const subscriptionName = 'dummySubscription';
  const server = setupServer(
    fetchRolesHandler({ roles: dummyRoles, path: '/roles' }),
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

  it('should fetch user roles from Hermes API without topic and subscription', async () => {
    // given
    server.listen();

    // when
    const { roles, loading, error } = useRoles(null, null);

    // then
    expect(loading.value).toBeTruthy();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchRoles).toBeNull();
      expect(roles.value).toEqual(dummyRoles);
    });
  });

  it('should fetch user roles from Hermes API with topic', async () => {
    // given
    server.use(
      fetchRolesHandler({
        roles: dummyRoles,
        path: `/roles/topics/${topicName}`,
      }),
    );
    server.listen();

    // when
    const { roles, loading, error } = useRoles(topicName, null);

    // then
    expect(loading.value).toBeTruthy();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchRoles).toBeNull();
      expect(roles.value).toEqual(dummyRoles);
    });
  });

  it('should fetch user roles from Hermes API with topic and subscription', async () => {
    // given
    server.use(
      fetchRolesHandler({
        roles: dummyRoles,
        path: `/roles/topics/${topicName}/subscriptions/${subscriptionName}`,
      }),
    );
    server.listen();

    // when
    const { roles, loading, error } = useRoles(topicName, subscriptionName);

    // then
    expect(loading.value).toBeTruthy();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchRoles).toBeNull();
      expect(roles.value).toEqual(dummyRoles);
    });
  });

  it('should set error to true on user roles endpoint failure', async () => {
    // given
    server.use(fetchRolesErrorHandler({ errorCode: 500, path: '/roles' }));
    server.listen();
    const notificationsStore = useNotificationsStore();
    const dispatchNotification = vi.spyOn(
      notificationsStore,
      'dispatchNotification',
    );

    // when
    const { loading, error } = useRoles(null, null);

    // then
    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchRoles).not.toBeNull();
      expect(dispatchNotification).toHaveBeenCalledOnce();
    });
  });
});
