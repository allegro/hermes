import { createTestingPiniaWithState } from '@/dummy/store';
import { dummySubscription } from '@/dummy/subscription';
import { dummyTopic } from '@/dummy/topic';
import { expect } from 'vitest';
import { render } from '@/utils/test-utils';
import ManageMessagesCard from '@/views/subscription/manage-messages-card/ManageMessagesCard.vue';
import userEvent from '@testing-library/user-event';

describe('ManageMessagesCard', () => {
  it('should render a card', () => {
    // when
    const { getByText } = render(ManageMessagesCard, {
      props: {
        topic: dummyTopic.name,
        subscription: dummySubscription.name,
        retransmitting: false,
        skippingAllMessages: false,
      },
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(getByText('subscription.manageMessagesCard.title')).toBeVisible();
    expect(getByText('subscription.manageMessagesCard.subtitle')).toBeVisible();
  });

  it('should open confirmation popup when retransmit button is clicked', async () => {
    // given
    const user = userEvent.setup();

    // when
    const { getByTestId, getByText } = render(ManageMessagesCard, {
      props: {
        topic: dummyTopic.name,
        subscription: dummySubscription.name,
        retransmitting: false,
        skippingAllMessages: false,
      },
      testPinia: createTestingPiniaWithState(),
    });
    await user.click(getByTestId('retransmitButton'));

    // then
    expect(
      getByText('subscription.confirmationDialog.retransmit.title'),
    ).toBeVisible();
  });

  it('should open confirmation popup when skip all messages button is clicked', async () => {
    const user = userEvent.setup();

    // when
    const { getByTestId, getByText } = render(ManageMessagesCard, {
      props: {
        topic: dummyTopic.name,
        subscription: dummySubscription.name,
        retransmitting: false,
        skippingAllMessages: false,
      },
      testPinia: createTestingPiniaWithState(),
    });
    await user.click(getByTestId('skipAllMessagesButton'));

    // then
    expect(
      getByText('subscription.confirmationDialog.skipAllMessages.title'),
    ).toBeVisible();
  });

  it('should disable buttons and show spinner when retransmission is in progress', async () => {
    // when
    const { getByTestId } = render(ManageMessagesCard, {
      props: {
        topic: dummyTopic.name,
        subscription: dummySubscription.name,
        retransmitting: true,
        skippingAllMessages: false,
      },
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(getByTestId('retransmitButton')).toBeDisabled();
    expect(getByTestId('skipAllMessagesButton')).toBeDisabled();
    expect(getByTestId('retransmitButton')).toHaveClass('v-btn--loading');
  });

  it('should disable buttons and show spinner when skipping all messages is in progress', async () => {
    // when
    const { getByTestId } = render(ManageMessagesCard, {
      props: {
        topic: dummyTopic.name,
        subscription: dummySubscription.name,
        retransmitting: false,
        skippingAllMessages: true,
      },
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(getByTestId('retransmitButton')).toBeDisabled();
    expect(getByTestId('skipAllMessagesButton')).toBeDisabled();
    expect(getByTestId('skipAllMessagesButton')).toContainElement(
      getByTestId('loading-spinner'),
    );
  });
});
