import { render } from '@/utils/test-utils';
import EnvironmentBadge from '@/components/environment-badge/EnviromentBadge.vue';

describe('EnvironmentBadge', () => {
  it('should render environment name badge', () => {
    // when
    const { getByText } = render(EnvironmentBadge, {
      props: {
        environmentName: 'prod',
      },
    });
    // then
    expect(getByText('PROD')).toBeInTheDocument();
  });
});
