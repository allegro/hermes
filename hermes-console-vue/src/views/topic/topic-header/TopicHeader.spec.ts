import { describe, expect } from 'vitest';
import { dummyTopic, dummyTopicOwner } from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import TopicHeader from '@/views/topic/topic-header/TopicHeader.vue';
import { createTestingPiniaWithState } from '@/dummy/store';

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
});
