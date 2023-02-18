import { dummyUndeliveredMessages } from '@/dummy/subscription';
import { formatTimestamp } from '@/utils/date-formatter/date-formatter';
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
    expect(getByText('Last 100 undelivered messages')).toBeInTheDocument();
  });

  it('should render undelivered messages table', () => {
    // when
    const { getByText } = render(UndeliveredMessagesCard, { props });

    // then
    props.undeliveredMessages.forEach((message, index) => {
      const row = getByText(index + 1).closest('tr')!;
      const expectedTimestamp = formatTimestamp(message.timestamp);

      expect(within(row).getByText(message.status)).toBeInTheDocument();
      expect(within(row).getByText(message.reason)).toBeInTheDocument();
      expect(within(row).getByText(expectedTimestamp)).toBeInTheDocument();
    });
  });
});
