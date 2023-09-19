import {
  appConfigStoreState,
  createTestingPiniaWithState,
} from '@/dummy/store';
import { createTestingPinia } from '@pinia/testing';
import { describe, expect } from 'vitest';
import { dummyAppConfig } from '@/dummy/app-config';
import { dummyOwner, dummyTopic } from '@/dummy/topic';
import { dummyRoles } from '@/dummy/roles';
import { render } from '@/utils/test-utils';
import { Role } from '@/api/role';
import { useOfflineRetransmission } from '@/composables/topic/use-offline-retransmission/useOfflineRetransmission';
import TopicHeader from '@/views/topic/topic-header/TopicHeader.vue';
import userEvent from '@testing-library/user-event';
import type { UseOfflineRetransmission } from '@/composables/topic/use-offline-retransmission/useOfflineRetransmission';

const useOfflineRetransmissionStub: UseOfflineRetransmission = {
  retransmit: () => Promise.resolve(true),
};

vi.mock(
  '@/composables/topic/use-offline-retransmission/useOfflineRetransmission',
);

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
    expect(
      getByText(`topicView.header.owner ${dummyOwner.name}`),
    ).toBeVisible();
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

  it('should open offline retransmission dialog when user clicks offline retransmission button', async () => {
    vi.mocked(useOfflineRetransmission).mockReturnValueOnce(
      useOfflineRetransmissionStub,
    );
    const user = userEvent.setup();

    // when
    const { getByTestId, queryByText } = render(TopicHeader, {
      props,
      testPinia: createTestingPinia({
        initialState: {
          appConfig: appConfigStoreState,
        },
      }),
    });

    // then
    await user.click(getByTestId('offlineRetransmissionButton'));
    expect(queryByText('offlineRetransmission.title')).toBeVisible();
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
          getByText('topicView.header.actions.export').closest('button'),
        ).toBeDisabled();
        expect(
          getByText('topicView.header.actions.remove').closest('button'),
        ).toBeDisabled();
      } else {
        expect(
          getByText('topicView.header.actions.edit').closest('button'),
        ).toBeEnabled();
        expect(
          getByText('topicView.header.actions.export').closest('button'),
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

  it.each([
    {
      offlineRetransmissionEnabled: false,
      offlineStorageEnabled: false,
      show: false,
    },
    {
      offlineRetransmissionEnabled: true,
      offlineStorageEnabled: false,
      show: false,
    },
    {
      offlineRetransmissionEnabled: false,
      offlineStorageEnabled: true,
      show: false,
    },
    {
      offlineRetransmissionEnabled: true,
      offlineStorageEnabled: true,
      show: true,
    },
  ])(
    'should show or hide offline retransmission button',
    ({ offlineRetransmissionEnabled, offlineStorageEnabled, show }) => {
      // when
      const testPinia = createTestingPinia({
        initialState: {
          appConfig: {
            ...appConfigStoreState,
            appConfig: {
              ...dummyAppConfig,
              topic: { ...dummyAppConfig.topic, offlineRetransmissionEnabled },
            },
          },
        },
      });
      const props = {
        topic: {
          ...dummyTopic,
          offlineStorage: {
            enabled: offlineStorageEnabled,
            retentionTime: {
              duration: 60,
              infinite: false,
            },
          },
        },
        owner: dummyOwner,
        roles: dummyRoles,
      };
      const { queryByText } = render(TopicHeader, {
        props,
        testPinia,
      });

      // then
      if (show) {
        expect(
          queryByText('topicView.header.actions.offlineRetransmission'),
        ).toBeVisible();
      } else {
        expect(
          queryByText('topicView.header.actions.offlineRetransmission'),
        ).not.toBeInTheDocument();
      }
    },
  );
});
