import { describe, expect } from 'vitest';
import { dummySubscription } from '@/dummy/subscription';
import { moveSubscriptionOffsetsHandler } from '@/mocks/handlers';
import { render } from '@/utils/test-utils';
import { setupServer } from 'msw/node';
import SubscriptionOffsets from '@/views/subscription/subscription-offsets/SubscriptionOffsets.vue';
import userEvent from '@testing-library/user-event';

describe('SubscriptionOffsets', () => {
  const props = { subscription: dummySubscription };
  const args = {
    topicName: dummySubscription.topicName,
    subscriptionName: dummySubscription.name,
    statusCode: 200,
  };

  const server = setupServer(moveSubscriptionOffsetsHandler(args));

  it('should show message that moving offsets was successful', async () => {
    // given
    server.listen();
    const user = userEvent.setup();
    const { getByText } = render(SubscriptionOffsets, { props });

    // when
    await user.click(getByText('subscription.moveOffsets.button'));

    // then
    expect(getByText('subscription.moveOffsets.success')).toBeVisible();
  });

  it('should show message that moving offsets failed', async () => {
    // given
    server.use(moveSubscriptionOffsetsHandler({ ...args, statusCode: 500 }));
    server.listen();
    const user = userEvent.setup();
    const { getByText } = render(SubscriptionOffsets, { props });

    // when
    await user.click(getByText('subscription.moveOffsets.button'));

    // then
    expect(
      getByText(
        'subscription.moveOffsets.failure, subscription.moveOffsets.status: 500, subscription.moveOffsets.response: ""',
      ),
    ).toBeVisible();
  });
});
