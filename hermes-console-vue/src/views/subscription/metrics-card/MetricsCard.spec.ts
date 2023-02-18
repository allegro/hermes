import { dummySubscriptionMetrics } from '@/dummy/subscription';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import MetricsCard from '@/views/subscription/metrics-card/MetricsCard.vue';

describe('MetricsCard', () => {
  const props = {
    subscriptionMetrics: dummySubscriptionMetrics,
  };

  it('should render metrics card', () => {
    // when
    const { getByText } = render(MetricsCard, { props });

    // then
    expect(getByText('Subscription metrics')).toBeInTheDocument();
  });

  it('should render subscription delivery rate', () => {
    // when
    const { getByText } = render(MetricsCard, { props });

    // then
    const deliveryRateRow = getByText('Delivery rate').closest('tr')!;
    expect(within(deliveryRateRow).getByText('0.00')).toBeInTheDocument();
  });

  it('should render subscription delivered events count', () => {
    // when
    const { getByText } = render(MetricsCard, { props });

    // then
    const deliveryRateRow = getByText('Delivered').closest('tr')!;
    expect(within(deliveryRateRow).getByText('39,099')).toBeInTheDocument();
  });

  it('should render subscription discarded events count', () => {
    // when
    const { getByText } = render(MetricsCard, { props });

    // then
    const deliveryRateRow = getByText('Discarded').closest('tr')!;
    expect(within(deliveryRateRow).getByText('2,137,086')).toBeInTheDocument();
  });

  it('should render subscription lag', () => {
    // when
    const { getByText } = render(MetricsCard, { props });

    // then
    const deliveryRateRow = getByText('Lag').closest('tr')!;
    expect(within(deliveryRateRow).getByText('9,055,513')).toBeInTheDocument();
  });
});
