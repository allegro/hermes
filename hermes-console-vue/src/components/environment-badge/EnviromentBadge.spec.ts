import { vuetifyRender } from '@/utils/test-utils';
import EnvironmentBadge from '@/components/environment-badge/EnviromentBadge.vue';

describe('EnvironmentBadge', () => {
  it('should render environment name badge', () => {
    // when
    const { getByText } = vuetifyRender(EnvironmentBadge, {
      props: {
        environmentName: 'prod',
      },
    });
    // then
    expect(getByText('PROD')).toBeTruthy();
  });
});
