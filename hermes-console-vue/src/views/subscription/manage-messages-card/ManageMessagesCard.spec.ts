import { render } from '@/utils/test-utils';
import ManageMessagesCard from '@/views/subscription/manage-messages-card/ManageMessagesCard.vue';

describe('ManageMessagesCard', () => {
  it('should render manage messages card', () => {
    // when
    const { getByText } = render(ManageMessagesCard);

    // then
    expect(
      getByText('subscription.manageMessagesCard.title'),
    ).toBeInTheDocument();
  });
});
