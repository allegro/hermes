import { afterEach } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import { dummyInconsistentTopics } from '@/dummy/inconsistentTopics';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import {
  fetchInconsistentTopicsErrorHandler,
  fetchInconsistentTopicsHandler,
  removeInconsistentTopicErrorHandler,
  removeInconsistentTopicHandler,
} from '@/mocks/handlers';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useInconsistentTopics } from '@/composables/inconsistent-topics/use-inconsistent-topics/useInconsistentTopics';
import { waitFor } from '@testing-library/vue';

describe('useInconsistentTopics', () => {
  const server = setupServer(
    fetchInconsistentTopicsHandler({ topics: dummyInconsistentTopics }),
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

  it('should fetch topics consistency details from Hermes API', async () => {
    // given
    server.listen();

    // when
    const { topics, loading, error } = useInconsistentTopics();

    // then
    expect(loading.value).toBeTruthy();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchInconsistentTopics).toBeNull();
      expect(topics.value).toEqual(
        expect.arrayContaining(dummyInconsistentTopics),
      );
    });
  });

  it('should set error to true on topics consistency endpoint failure', async () => {
    // given
    server.use(fetchInconsistentTopicsErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { loading, error } = useInconsistentTopics();

    // then
    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchInconsistentTopics).not.toBeNull();
    });
  });

  it('should show message that removing inconsistentTopic was successful', async () => {
    // given
    server.use(removeInconsistentTopicHandler());
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { removeInconsistentTopic } = useInconsistentTopics();

    // when
    await removeInconsistentTopic(dummyInconsistentTopics[0]);

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'success',
        text: 'notifications.inconsistentTopic.delete.success',
      });
    });
  });

  it('should show message that removing inconsistentTopic was unsuccessful', async () => {
    // given
    server.use(removeInconsistentTopicErrorHandler({ errorCode: 500 }));
    server.listen();
    const notificationStore = notificationStoreSpy();

    const { removeInconsistentTopic } = useInconsistentTopics();

    // when
    await removeInconsistentTopic(dummyInconsistentTopics[0]);

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.inconsistentTopic.delete.failure',
      });
    });
  });
});
