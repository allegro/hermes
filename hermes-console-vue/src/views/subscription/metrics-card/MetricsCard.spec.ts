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
    expect(getByText('subscription.metricsCard.title')).toBeInTheDocument();
  });

  it('should render subscription delivery rate', () => {
    // when
    const { getByText } = render(MetricsCard, { props });

    // then
    const deliveryRateRow = getByText(
      'subscription.metricsCard.deliveryRate',
    ).closest('tr')!;
    expect(within(deliveryRateRow).getByText('0.00')).toBeInTheDocument();
  });

  it('should render subscription delivered events count', () => {
    // when
    const { getByText } = render(MetricsCard, { props });

    // then
    const deliveryRateRow = getByText(
      'subscription.metricsCard.delivered',
    ).closest('tr')!;
    expect(within(deliveryRateRow).getByText('39,099')).toBeInTheDocument();
  });

  it('should render subscription discarded events count', () => {
    // when
    const { getByText } = render(MetricsCard, { props });

    // then
    const deliveryRateRow = getByText(
      'subscription.metricsCard.discarded',
    ).closest('tr')!;
    expect(within(deliveryRateRow).getByText('2,137,086')).toBeInTheDocument();
  });

  it('should render subscription lag', () => {
    // when
    const { getByText } = render(MetricsCard, { props });

    // then
    const deliveryRateRow = getByText('subscription.metricsCard.lag').closest(
      'tr',
    )!;
    expect(within(deliveryRateRow).getByText('9,055,513')).toBeInTheDocument();
  });
});
