import {
  appConfigStoreState,
  createTestingPiniaWithState,
} from '@/dummy/store';
import { createTestingPinia } from '@pinia/testing';
import { describe, expect } from 'vitest';
import { dummyAppConfig } from '@/dummy/app-config';
import { dummyDataSources } from '@/dummy/topic-form';
import {
  dummyInitializedTopicForm,
  dummyTopicFormValidator,
} from '@/dummy/topic-form';
import { dummyOwner, dummyTopic } from '@/dummy/topic';
import { dummyRoles } from '@/dummy/roles';
import { fireEvent } from '@testing-library/vue';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { Role } from '@/api/role';
import { useEditTopic } from '@/composables/topic/use-edit-topic/useEditTopic';
import TopicHeader from '@/views/topic/topic-header/TopicHeader.vue';
import type { UseEditTopic } from '@/composables/topic/use-edit-topic/types';

vi.mock(
  '@/composables/topic/use-offline-retransmission/useOfflineRetransmission',
);

vi.mock('@/composables/topic/use-edit-topic/useEditTopic');

const useEditTopicStub: UseEditTopic = {
  form: ref(dummyInitializedTopicForm),
  validators: dummyTopicFormValidator,
  dataSources: dummyDataSources,
  createOrUpdateTopic: () => Promise.resolve(true),
  creatingOrUpdatingTopic: ref(false),
  errors: ref({
    fetchOwners: null,
    fetchOwnerSources: null,
  }),
};

describe('TopicHeader', () => {
  const props = {
    topic: dummyTopic,
    owner: dummyOwner,
    roles: dummyRoles,
  };

  it('should render basic topic information', () => {
    // when
    const { getByText } = render(TopicHeader, {
      props,
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(getByText('topicView.header.topic')).toBeVisible();
    expect(getByText(dummyTopic.name)).toBeVisible();
    expect(getByText(`topicView.header.owner`)).toBeVisible();
    expect(getByText(dummyOwner.name)).toBeVisible();
    expect(getByText(dummyTopic.description)).toBeVisible();
  });

  it('should show add topic to favorites button', async () => {
    // when
    const { getByText, queryByText } = render(TopicHeader, {
      props,
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(
      getByText('topicView.header.actions.addToFavorites'),
    ).toBeInTheDocument();
    expect(
      queryByText('topicView.header.actions.removeFromFavorites'),
    ).not.toBeInTheDocument();
  });

  it('should show remove topic from favorites button', async () => {
    // when
    const { getByText, queryByText } = render(TopicHeader, {
      props,
      testPinia: createTestingPinia({
        initialState: {
          favorites: {
            topics: [dummyTopic.name],
          },
          appConfig: appConfigStoreState,
        },
      }),
    });

    // then
    expect(
      getByText('topicView.header.actions.removeFromFavorites'),
    ).toBeInTheDocument();
    expect(
      queryByText('topicView.header.actions.addToFavorites'),
    ).not.toBeInTheDocument();
  });

  it.each([
    { roles: [], disabled: true },
    { roles: [Role.ANY], disabled: true },
    { roles: [Role.ADMIN], disabled: false },
    { roles: [Role.TOPIC_OWNER], disabled: false },
  ])(
    'should disable or enable buttons based on user authorization',
    ({ roles, disabled }) => {
      // when
      const testProps = {
        topic: dummyTopic,
        owner: dummyOwner,
        roles: roles,
      };
      const { getByText } = render(TopicHeader, {
        props: testProps,
        testPinia: createTestingPiniaWithState(),
      });

      // then
      if (disabled) {
        expect(
          getByText('topicView.header.actions.edit').closest('button'),
        ).toBeDisabled();
        expect(
          getByText('topicView.header.actions.remove').closest('button'),
        ).toBeDisabled();
      } else {
        expect(
          getByText('topicView.header.actions.edit').closest('button'),
        ).toBeEnabled();
        expect(
          getByText('topicView.header.actions.remove').closest('button'),
        ).toBeEnabled();
      }
    },
  );

  it.each([{ readOnlyModeEnabled: false }, { readOnlyModeEnabled: true }])(
    'should disable or enable edit button based on read only mode',
    ({ readOnlyModeEnabled }) => {
      // when
      const testPinia = createTestingPinia({
        initialState: {
          appConfig: {
            ...appConfigStoreState,
            appConfig: {
              ...dummyAppConfig,
              topic: { ...dummyAppConfig.topic, readOnlyModeEnabled },
            },
          },
        },
      });
      const { getByText } = render(TopicHeader, {
        props,
        testPinia,
      });

      // then
      if (readOnlyModeEnabled) {
        expect(
          getByText('topicView.header.actions.edit').closest('button'),
        ).toBeDisabled();
      } else {
        expect(
          getByText('topicView.header.actions.edit').closest('button'),
        ).toBeEnabled();
      }
    },
  );

  it('should show edit topic dialog on button click', async () => {
    // given
    vi.mocked(useEditTopic).mockReturnValueOnce(useEditTopicStub);

    // when
    const { getByText } = render(TopicHeader, {
      testPinia: createTestingPiniaWithState(),
      props,
    });
    await fireEvent.click(getByText('topicView.header.actions.edit'));

    // then
    expect(getByText('topicView.header.editTopic')).toBeInTheDocument();
    expect(getByText('topicForm.actions.update')).toBeInTheDocument();
  });
});
