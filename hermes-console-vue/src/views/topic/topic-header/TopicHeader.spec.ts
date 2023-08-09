import { describe, expect } from 'vitest';
import { dummyTopic, dummyTopicOwner } from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import TopicHeader from '@/views/topic/topic-header/TopicHeader.vue';
import {
  appConfigStoreState,
  createTestingPiniaWithState,
} from '@/dummy/store';
import { createTestingPinia } from '@pinia/testing';
import { dummyAppConfig } from '@/dummy/app-config';

describe('TopicHeader', () => {
  const props = { topic: dummyTopic, owner: dummyTopicOwner };

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
      getByText(`topicView.header.owner ${dummyTopicOwner.name}`),
    ).toBeVisible();
    expect(getByText(dummyTopic.description)).toBeVisible();
  });

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
        owner: dummyTopicOwner,
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
