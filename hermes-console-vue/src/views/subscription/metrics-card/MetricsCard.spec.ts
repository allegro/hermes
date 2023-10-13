import { createTestingPiniaWithState } from '@/dummy/store';
import { dummyMetricsDashboardUrl } from '@/dummy/metricsDashboardUrl';
import {
  dummySubscription,
  dummySubscriptionMetrics,
} from '@/dummy/subscription';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useMetrics } from '@/composables/metrics/use-metrics/useMetrics';
import { within } from '@testing-library/vue';
import MetricsCard from '@/views/subscription/metrics-card/MetricsCard.vue';
import type { UseMetrics } from '@/composables/metrics/use-metrics/useMetrics';

vi.mock('@/composables/metrics/use-metrics/useMetrics');

const useMetricsStub: UseMetrics = {
  dashboardUrl: ref(dummyMetricsDashboardUrl.url),
  loading: ref(false),
  error: ref({
    fetchDashboardUrl: null,
  }),
};

describe('MetricsCard', () => {
  const props = {
    subscriptionMetrics: dummySubscriptionMetrics,
    topicName: dummySubscription.topicName,
    subscriptionName: dummySubscription.name,
  };

  it('should render metrics card', () => {
    // when
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    const { getByText } = render(MetricsCard, {
      props,
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(getByText('subscription.metricsCard.title')).toBeVisible();
  });

  it('should render subscription delivery rate', () => {
    // when
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    const { getByText } = render(MetricsCard, {
      props,
      testPinia: createTestingPiniaWithState(),
    });

    // then
    const deliveryRateRow = getByText(
      'subscription.metricsCard.deliveryRate',
    ).closest('tr')!;
    expect(within(deliveryRateRow).getByText('0.00')).toBeVisible();
  });

  it('should render subscription delivered events count', () => {
    // when
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    const { getByText } = render(MetricsCard, {
      props,
      testPinia: createTestingPiniaWithState(),
    });

    // then
    const deliveryRateRow = getByText(
      'subscription.metricsCard.delivered',
    ).closest('tr')!;
    expect(within(deliveryRateRow).getByText('39,099')).toBeVisible();
  });

  it('should render subscription discarded events count', () => {
    // when
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    const { getByText } = render(MetricsCard, {
      props,
      testPinia: createTestingPiniaWithState(),
    });

    // then
    const deliveryRateRow = getByText(
      'subscription.metricsCard.discarded',
    ).closest('tr')!;
    expect(within(deliveryRateRow).getByText('2,137,086')).toBeVisible();
  });

  it('should render subscription lag', () => {
    // when
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    const { getByText } = render(MetricsCard, {
      props,
      testPinia: createTestingPiniaWithState(),
    });

    // then
    const deliveryRateRow = getByText('subscription.metricsCard.lag').closest(
      'tr',
    )!;
    expect(within(deliveryRateRow).getByText('9,055,513')).toBeVisible();
  });
});
