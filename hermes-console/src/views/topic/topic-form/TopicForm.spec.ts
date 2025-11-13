import { afterEach } from 'vitest';
import { createTestingPiniaWithState } from '@/dummy/store';
import { dummyOwnerSources } from '@/dummy/topic-form';
import { dummyTopic } from '@/dummy/topic';
import { expect } from 'vitest';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
  render,
} from '@/utils/test-utils';
import { fetchOwnerHandler, fetchOwnerSourcesHandler } from '@/mocks/handlers';
import { groupName } from '@/utils/topic-utils/topic-utils';
import { setupServer } from 'msw/node';
import { waitFor } from '@testing-library/vue';
import TopicForm from '@/views/topic/topic-form/TopicForm.vue';
import userEvent from '@testing-library/user-event';

describe('TopicForm', () => {
  const server = setupServer(fetchOwnerSourcesHandler(dummyOwnerSources));

  afterEach(() => {
    server.resetHandlers();
  });

  const props = {
    group: groupName(dummyTopic.name),
    topic: dummyTopic,
    operation: 'add',
    modelValue: true,
    roles: [],
  };

  it('renders properly', () => {
    // given
    server.listen();

    // when
    const { getByText, getAllByText, queryByText } = render(TopicForm, {
      testPinia: createTestingPiniaWithState(),
      props,
    });

    // then
    expect(getAllByText('topicForm.fields.name.label')[0]).toBeVisible();
    expect(getAllByText('topicForm.fields.description.label')[0]).toBeVisible();
    expect(getAllByText('topicForm.fields.ownerSource.label')[0]).toBeVisible();
    expect(getAllByText('topicForm.fields.owner.label')[0]).toBeVisible();
    expect(getAllByText('topicForm.fields.auth.enabled')[0]).toBeVisible();
    expect(getAllByText('topicForm.fields.contentType')[0]).toBeVisible();
    expect(
      getAllByText('topicForm.fields.auth.unauthenticatedAccessEnabled')[0],
    ).toBeVisible();
    expect(
      getAllByText('topicForm.fields.restrictSubscribing')[0],
    ).toBeVisible();
    expect(getAllByText('topicForm.fields.trackingEnabled')[0]).toBeVisible();
    expect(
      getAllByText('topicForm.fields.retentionTime.unit')[0],
    ).toBeVisible();
    expect(
      getAllByText('topicForm.fields.retentionTime.duration')[0],
    ).toBeVisible();
    expect(getAllByText('topicForm.fields.ack')[0]).toBeVisible();
    expect(
      getAllByText('topicForm.fields.maxMessageSize.label')[0],
    ).toBeVisible();
    expect(getAllByText('topicForm.fields.storeOffline')[0]).toBeVisible();
    expect(queryByText('topicForm.actions.update')).not.toBeInTheDocument();

    expect(
      getByText('topicForm.actions.create').closest('button'),
    ).toBeEnabled();
    expect(
      getByText('topicForm.actions.cancel').closest('button'),
    ).toBeEnabled();
  });

  it('should render in edit mode', () => {
    // given
    server.use(fetchOwnerHandler({}));
    server.listen();

    // when
    const { getByText, queryByText } = render(TopicForm, {
      testPinia: createTestingPiniaWithState(),
      props: {
        ...props,
        operation: 'edit',
        topic: dummyTopic,
      },
    });

    // then
    expect(queryByText('topicForm.actions.create')).not.toBeInTheDocument();

    expect(
      getByText('topicForm.actions.update').closest('button'),
    ).toBeEnabled();
    expect(
      getByText('topicForm.actions.cancel').closest('button'),
    ).toBeEnabled();
  });

  it('should dispatch notification about validation error', async () => {
    // given
    server.listen();
    const { getByText } = render(TopicForm, {
      testPinia: createTestingPiniaWithState(),
      props,
    });
    const notificationStore = notificationStoreSpy();

    // when
    await userEvent.click(getByText('topicForm.actions.create'));

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.form.validationError',
        text: '',
      });
    });
  });
});
