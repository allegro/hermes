import { createTestingPinia } from '@pinia/testing';
import { describe, expect } from 'vitest';
import { dummyTopic } from '@/dummy/topic';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import {
  subscriptionFilterVerificationErrorHandler,
  subscriptionFilterVerificationHandler,
  successfulSubscriptionHandlers,
} from '@/mocks/handlers';
import { useSubscriptionFiltersDebug } from '@/composables/subscription/use-subscription-filters-debug/useSubscriptionFiltersDebug';
import { VerificationStatus } from '@/api/message-filters-verification';
import { waitFor } from '@testing-library/vue';

describe('useSubscriptionFiltersDebug', () => {
  const server = setupServer(...successfulSubscriptionHandlers);

  const pinia = createTestingPinia({
    fakeApp: true,
  });

  beforeEach(() => {
    setActivePinia(pinia);
  });

  afterEach(() => {
    server.resetHandlers();
  });

  it('should dispatch error notification when verification fails with bad http status code', async () => {
    // given
    server.use(
      subscriptionFilterVerificationErrorHandler({
        topicName: dummyTopic.name,
      }),
    );
    server.listen();
    const notificationsStore = notificationStoreSpy();

    // when
    const { verify } = useSubscriptionFiltersDebug();
    await verify(dummyTopic.name, [], 'message');

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationsStore, {
        type: 'error',
        title: 'notifications.subscriptionFiltersDebug.failure',
      });
    });
  });

  it('should set status and error message when verification fails', async () => {
    // given
    server.use(
      subscriptionFilterVerificationHandler({
        topicName: dummyTopic.name,
        response: {
          status: VerificationStatus.ERROR,
          errorMessage: 'error',
        },
      }),
    );
    server.listen();

    // when
    const { errorMessage, status, verify } = useSubscriptionFiltersDebug();
    await verify(dummyTopic.name, [], 'message');

    // then
    await waitFor(() => {
      expect(status.value).toBe('ERROR');
      expect(errorMessage.value).toBe('error');
    });
  });

  it('should set status when verification succeeds', async () => {
    // given
    server.use(
      subscriptionFilterVerificationHandler({
        topicName: dummyTopic.name,
        response: {
          status: VerificationStatus.MATCHED,
        },
      }),
    );
    server.listen();

    // when
    const { errorMessage, status, verify } = useSubscriptionFiltersDebug();
    await verify(dummyTopic.name, [], 'message');

    // then
    await waitFor(() => {
      expect(status.value).toBe('MATCHED');
      expect(errorMessage.value).toBeUndefined();
    });
  });
});
