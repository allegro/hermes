import { describe, expect } from 'vitest';
import {
  dummySubscription,
  secondDummySubscription,
} from '@/dummy/subscription';
import { render } from '@/utils/test-utils';
import SubscriptionsList from '@/views/topic/subscriptions-list/SubscriptionsList.vue';
import userEvent from '@testing-library/user-event';
import {beforeEach} from "vitest";
import router from "@/router";

describe('SubscriptionsList', () => {
  const props = {
    groupId: 'pl.allegro',
    topicName: 'pl.allegro.DummyTopic',
    subscriptions: [dummySubscription, secondDummySubscription],
  };

  beforeEach(async () => {
    await router.push(
        `/ui/groups/${props.groupId}/topics/${props.topicName}`,
    );
  });

  it('should render proper heading', () => {
    // when
    const { getByText } = render(SubscriptionsList, { props });

    // then
    expect(getByText('topicView.subscriptions.title (2)')).toBeVisible();
  });

  it.each(props.subscriptions)(
    'should render button %s representing each subscription',
    async (subscription) => {
      // when
      const { getByText } = render(SubscriptionsList, { props });
      await userEvent.click(getByText('topicView.subscriptions.title (2)'));

      // then
      expect(getByText(subscription.name)).toBeVisible();
      expect(getByText(subscription.name).closest('a')).toHaveAttribute(
        'href',
        `${window.location.href}/subscriptions/${subscription.name}`,
      );
    },
  );
});
