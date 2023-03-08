import { dummyUndeliveredMessage } from '@/dummy/subscription';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import LastUndeliveredMessage from '@/views/subscription/last-undelivered-message/LastUndeliveredMessage.vue';

describe('LastUndeliveredMessage', () => {
  const props = {
    lastUndelivered: dummyUndeliveredMessage,
  };

  it('should render last undelivered message card', () => {
    // when
    const { getByText } = render(LastUndeliveredMessage, { props });

    // then
    expect(
      getByText('subscription.lastUndeliveredMessage.title'),
    ).toBeInTheDocument();
  });

  it('should render last undelivered message formatted time', () => {
    // when
    const { getByText } = render(LastUndeliveredMessage, { props });

    // then
    const timeRow = getByText(
      'subscription.lastUndeliveredMessage.time',
    ).closest('tr')!;
    expect(
      within(timeRow).getByText('2009-02-13 23:31:30'),
    ).toBeInTheDocument();
  });

  it('should render last undelivered message reason', () => {
    // when
    const { getByText } = render(LastUndeliveredMessage, { props });

    // then
    const reasonRow = getByText(
      'subscription.lastUndeliveredMessage.reason',
    ).closest('tr')!;
    expect(
      within(reasonRow).getByText(
        'Message sending failed with status code: 500',
      ),
    ).toBeInTheDocument();
  });

  it('should render last undelivered message content', () => {
    // when
    const { getByText } = render(LastUndeliveredMessage, { props });

    // then
    const messageRow = getByText(
      'subscription.lastUndeliveredMessage.message',
    ).closest('tr')!;
    expect(
      within(messageRow).getByText(
        '{"id":"123","foo":[1,2,3],"bar":{"qaz":42}}',
      ),
    ).toBeInTheDocument();
  });
});
