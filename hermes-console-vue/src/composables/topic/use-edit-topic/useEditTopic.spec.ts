import { afterEach, expect } from 'vitest';
import { createTestingPiniaWithState } from '@/dummy/store';
import {
  dummyInitializedEditTopicForm,
  dummyOwnerSources,
} from '@/dummy/topic-form';
import { dummyTopic } from '@/dummy/topic';
import {
  editTopicErrorHandler,
  editTopicHandler,
  fetchOwnerHandler,
  fetchOwnerSourcesHandler,
} from '@/mocks/handlers';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useEditTopic } from '@/composables/topic/use-edit-topic/useEditTopic';
import { waitFor } from '@testing-library/vue';

describe('useEditTopic', () => {
  const server = setupServer(
    fetchOwnerSourcesHandler(dummyOwnerSources),
    fetchOwnerHandler({}),
  );

  beforeEach(() => {
    setActivePinia(createTestingPiniaWithState());
  });

  afterEach(() => {
    server.resetHandlers();
  });

  it('should return initialized form', async () => {
    //given
    server.listen();

    // when
    const { form } = useEditTopic(dummyTopic);

    // then
    expect(JSON.stringify(form.value)).toMatchObject(
      JSON.stringify(dummyInitializedEditTopicForm),
    );
  });

  it('should dispatch notification about topic edit request error', async () => {
    //given
    server.use(editTopicErrorHandler(dummyTopic.name, 500));
    server.listen();
    const notificationStore = notificationStoreSpy();
    const { createOrUpdateTopic } = useEditTopic(dummyTopic);

    // when
    await createOrUpdateTopic();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        title: 'notifications.topic.edit.failure',
        type: 'error',
      });
    });
  });

  it('should dispatch notification about topic edit success', async () => {
    //given
    server.use(editTopicHandler(dummyTopic.name));
    server.listen();
    const notificationStore = notificationStoreSpy();
    const { createOrUpdateTopic } = useEditTopic(dummyTopic);

    // when
    await createOrUpdateTopic();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        text: 'notifications.topic.edit.success',
        type: 'success',
      });
    });
  });
});
