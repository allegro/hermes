import { dummyUndeliveredMessages } from '@/dummy/subscription';
import { formatTimestampMillis } from '@/utils/date-formatter/date-formatter';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import UndeliveredMessagesCard from '@/views/subscription/undelivered-messages-card/UndeliveredMessagesCard.vue';

describe('UndeliveredMessagesCard', () => {
  const props = {
    undeliveredMessages: dummyUndeliveredMessages,
  };

  it('should render undelivered messages card', () => {
    // when
    const { getByText } = render(UndeliveredMessagesCard, { props });

    // then
    expect(
      getByText('subscription.undeliveredMessagesCard.title'),
    ).toBeVisible();
  });

  it('should render undelivered messages table', () => {
    // when
    const { getByText } = render(UndeliveredMessagesCard, { props });

    // then
    props.undeliveredMessages.forEach((message, index) => {
      const row = getByText(index + 1).closest('tr')!;
      const expectedTimestamp = formatTimestampMillis(message.timestamp);

      expect(within(row).getByText(message.status)).toBeVisible();
      expect(within(row).getByText(message.reason)).toBeVisible();
      expect(within(row).getByText(expectedTimestamp)).toBeVisible();
    });
  });
});
