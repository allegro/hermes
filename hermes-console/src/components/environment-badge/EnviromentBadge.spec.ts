import { expect } from 'vitest';
import { render } from '@/utils/test-utils';
import EnvironmentBadge from '@/components/environment-badge/EnviromentBadge.vue';

describe('EnvironmentBadge', () => {
  it('should render environment name badge', () => {
    // when
    const { getByText } = render(EnvironmentBadge, {
      props: {
        environmentName: 'prod',
        isCriticalEnvironment: false,
      },
    });

    // then
    expect(getByText('PROD')).toBeVisible();
  });
});
