import { afterEach, describe, expect } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import {
  syncGroupHandler,
  syncSubscriptionHandler,
  syncTopicHandler,
} from '@/mocks/handlers';
import { useSync } from '@/composables/sync/use-sync/useSync';
import { waitFor } from '@testing-library/vue';

describe('useSync', () => {
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

  it('should show error notification when group sync fails', async () => {
    // given
    const groupName = 'group';
    server.use(syncGroupHandler({ groupName, statusCode: 500 }));
    server.listen();

    const notificationStore = notificationStoreSpy();

    // when
    const { syncGroup } = useSync();
    const result = await syncGroup(groupName, 'DC1');

    // then
    expect(result).toBeFalsy();

    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.consistency.sync.failure',
      });
    });
  });

  it('should show error notification when topic sync fails', async () => {
    // given
    const topicName = 'group.topic';
    server.use(syncTopicHandler({ topicName, statusCode: 500 }));
    server.listen();

    const notificationStore = notificationStoreSpy();

    // when
    const { syncTopic } = useSync();
    const result = await syncTopic(topicName, 'DC1');

    // then
    expect(result).toBeFalsy();

    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.consistency.sync.failure',
      });
    });
  });

  it('should show error notification when subscription sync fails', async () => {
    // given
    const topicName = 'group.topic';
    const subscriptionName = 'subscription';
    server.use(
      syncSubscriptionHandler({ topicName, subscriptionName, statusCode: 500 }),
    );
    server.listen();

    const notificationStore = notificationStoreSpy();

    // when
    const { syncSubscription } = useSync();
    const result = await syncSubscription(topicName, subscriptionName, 'DC1');

    // then
    expect(result).toBeFalsy();

    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.consistency.sync.failure',
      });
    });
  });

  it('should show success notification when group sync is successful', async () => {
    const groupName = 'group';

    server.use(syncGroupHandler({ groupName, statusCode: 200 }));
    server.listen();

    const notificationStore = notificationStoreSpy();

    // when
    const { syncGroup } = useSync();
    const result = await syncGroup(groupName, 'DC1');

    // then
    expect(result).toBeTruthy();

    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        text: 'notifications.consistency.sync.success',
      });
    });
  });

  it('should show success notification when topic sync is successful', async () => {
    // given
    const topicName = 'group.topic';
    server.use(syncTopicHandler({ topicName, statusCode: 200 }));
    server.listen();

    const notificationStore = notificationStoreSpy();

    // when
    const { syncTopic } = useSync();
    const result = await syncTopic(topicName, 'DC1');

    // then
    expect(result).toBeTruthy();

    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        text: 'notifications.consistency.sync.success',
      });
    });
  });

  it('should show success notification when subscription sync is successful', async () => {
    // given
    const topicName = 'group.topic';
    const subscriptionName = 'subscription';
    server.use(
      syncSubscriptionHandler({ topicName, subscriptionName, statusCode: 200 }),
    );
    server.listen();

    const notificationStore = notificationStoreSpy();

    // when
    const { syncSubscription } = useSync();
    const result = await syncSubscription(topicName, subscriptionName, 'DC1');

    // then
    expect(result).toBeTruthy();

    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        text: 'notifications.consistency.sync.success',
      });
    });
  });
});
