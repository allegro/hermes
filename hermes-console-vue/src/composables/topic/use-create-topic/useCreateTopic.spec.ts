import { afterEach, expect } from 'vitest';
import { createTestingPiniaWithState } from '@/dummy/store';
import {
  createTopicErrorHandler,
  createTopicHandler,
  fetchOwnerSourcesHandler,
} from '@/mocks/handlers';
import {
  dummyInitializedTopicForm,
  dummyOwnerSources,
} from '@/dummy/topic-form';
import { dummyTopic } from '@/dummy/topic';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import { groupName } from '@/utils/topic-utils/topic-utils';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useCreateTopic } from '@/composables/topic/use-create-topic/useCreateTopic';
import { waitFor } from '@testing-library/vue';

describe('useCreateTopic', () => {
  const server = setupServer(fetchOwnerSourcesHandler(dummyOwnerSources));

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
    const { form } = useCreateTopic(groupName(dummyTopic.name));

    // then
    expect(JSON.stringify(form.value)).toMatchObject(
      JSON.stringify(dummyInitializedTopicForm),
    );
  });

  it('should dispatch notification about topic creation request error', async () => {
    //given
    // @ts-ignore
    server.use(createTopicErrorHandler(500));
    server.listen();
    const notificationStore = notificationStoreSpy();
    const { createOrUpdateTopic } = useCreateTopic(groupName(dummyTopic.name));

    // when
    await createOrUpdateTopic();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        title: 'notifications.topic.create.failure',
        type: 'error',
      });
    });
  });

  it('should dispatch notification about topic creation success', async () => {
    //given
    server.use(createTopicHandler());
    server.listen();
    const notificationStore = notificationStoreSpy();
    const { createOrUpdateTopic } = useCreateTopic(groupName(dummyTopic.name));

    // when
    await createOrUpdateTopic();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        text: 'notifications.topic.create.success',
        type: 'success',
      });
    });
  });
});
