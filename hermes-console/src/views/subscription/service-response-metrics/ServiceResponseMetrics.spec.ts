import { render } from '@/utils/test-utils';
import ServiceResponseMetrics from '@/views/subscription/service-response-metrics/ServiceResponseMetrics.vue';

describe('ServiceResponseMetrics', () => {
  it('should render service response metrics card', () => {
    // when
    const { getByText } = render(ServiceResponseMetrics);

    // then
    expect(
      getByText('subscription.serviceResponseMetrics.title'),
    ).toBeVisible();
    expect(getByText('subscription.serviceResponseMetrics.2xx')).toBeVisible();
    expect(getByText('subscription.serviceResponseMetrics.4xx')).toBeVisible();
    expect(getByText('subscription.serviceResponseMetrics.5xx')).toBeVisible();
    expect(
      getByText('subscription.serviceResponseMetrics.networkTimeouts'),
    ).toBeVisible();
    expect(
      getByText('subscription.serviceResponseMetrics.otherNetworkErrors'),
    ).toBeVisible();
  });
});
