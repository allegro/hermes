import { render } from '@/utils/test-utils';
import ShowEventTrace from '@/views/subscription/show-event-trace/ShowEventTrace.vue';

describe('ShowEventTrace', () => {
  it('should render show event trace card', () => {
    // when
    const { getByText } = render(ShowEventTrace);

    // then
    expect(getByText('subscription.showEventTrace.title')).toBeInTheDocument();
  });
});
