import { afterEach } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import { dummyDatacentersReadiness } from '@/dummy/readiness';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import {
  fetchReadinessErrorHandler,
  fetchReadinessHandler,
  switchReadinessErrorHandler,
  switchReadinessHandler,
} from '@/mocks/handlers';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useReadiness } from '@/composables/readiness/use-readiness/useReadiness';
import { waitFor } from '@testing-library/vue';

describe('useReadiness', () => {
  const server = setupServer(
    fetchReadinessHandler({ datacentersReadiness: dummyDatacentersReadiness }),
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

  afterEach(() => {
    server.resetHandlers();
  });

  it('should fetch readiness details from Hermes API', async () => {
    // given
    server.listen();

    // when
    const { datacentersReadiness, loading, error } = useReadiness();

    // then
    expect(loading.value).toBeTruthy();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchReadiness).toBeNull();
      expect(datacentersReadiness.value).toEqual(dummyDatacentersReadiness);
    });
  });

  it('should set error to true on datacenters readiness endpoint failure', async () => {
    // given
    server.use(fetchReadinessErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { loading, error } = useReadiness();

    // then
    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchReadiness).not.toBeNull();
    });
  });

  it('should show message that switching readiness subscription was successful', async () => {
    // given
    server.use(
      switchReadinessHandler({
        datacenter: dummyDatacentersReadiness[0].datacenter,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { switchReadinessState } = useReadiness();

    // when
    await switchReadinessState(dummyDatacentersReadiness[0].datacenter, true);

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        text: 'notifications.readiness.switch.success',
      });
    });
  });

  it('should show message that switching readiness subscription was unsuccessful', async () => {
    // given
    server.use(
      switchReadinessErrorHandler({
        datacenter: dummyDatacentersReadiness[0].datacenter,
        errorCode: 500,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { switchReadinessState } = useReadiness();

    // when
    await switchReadinessState(dummyDatacentersReadiness[0].datacenter, true);

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.readiness.switch.failure',
      });
    });
  });
});
