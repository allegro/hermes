import { render } from '@/utils/test-utils';
import ServiceResponseMetrics from '@/views/subscription/service-response-metrics/ServiceResponseMetrics.vue';

describe('ServiceResponseMetrics', () => {
  it('should render service response metrics card', () => {
    // when
    const { getByText } = render(ServiceResponseMetrics);

    // then
    expect(getByText('Service response metrics')).toBeInTheDocument();
    expect(getByText('2xx')).toBeInTheDocument();
    expect(getByText('4xx')).toBeInTheDocument();
    expect(getByText('5xx')).toBeInTheDocument();
    expect(getByText('Network timeouts')).toBeInTheDocument();
    expect(getByText('Other network errors')).toBeInTheDocument();
  });
});
