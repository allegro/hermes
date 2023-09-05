import { afterEach, describe } from 'vitest';
import {
  createRetransmissionTaskHandler,
} from '@/mocks/handlers';
import { createTestingPinia } from '@pinia/testing';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useOfflineRetransmission } from '@/composables/topic/use-offline-retransmission/useOfflineRetransmission';
import { waitFor } from '@testing-library/vue';

describe('useOfflineRetransmission', () => {
  const server = setupServer();

  const pinia = createTestingPinia({
    fakeApp: true,
  });

  beforeEach(() => {
    setActivePinia(pinia);
  });

  afterEach(() => {
    server.resetHandlers();
  });

  it('should dispatch notification on successful task create', async () => {
    // given
    server.use(
      createRetransmissionTaskHandler({
        statusCode: 200,
      }),
    );
    server.listen();
    const notificationsStore = notificationStoreSpy();

    // when
    const { retransmit } = useOfflineRetransmission();
    const success = await retransmit({
      sourceTopic: 'sourceTopic',
      targetTopic: 'targetTopic',
      startTimestamp: '2023-01-06T12:00:00Z',
      endTimestamp: '2023-01-12T12:00:00Z',
    });

    // then
    expect(success).toBeTruthy();
    await waitFor(() => {
      expectNotificationDispatched(notificationsStore, {
        type: 'success',
        title: 'notifications.offlineRetransmission.create.success',
      });
    });
  });

  it('should dispatch notification on unsuccessful task crate', async () => {
    // given
    server.use(
      createRetransmissionTaskHandler({
        statusCode: 500,
      }),
    );
    server.listen();
    const notificationsStore = notificationStoreSpy();

    // when
    const { retransmit } = useOfflineRetransmission();
    const success = await retransmit({
      sourceTopic: 'sourceTopic',
      targetTopic: 'targetTopic',
      startTimestamp: '2023-01-06T12:00:00Z',
      endTimestamp: '2023-01-12T12:00:00Z',
    });

    // then
    expect(success).toBeFalsy();
    await waitFor(() => {
      expectNotificationDispatched(notificationsStore, {
        type: 'error',
        title: 'notifications.offlineRetransmission.create.failure',
      });
    });
  });
});
