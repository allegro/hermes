import { appConfigStoreState } from '@/dummy/store';
import { createTestingPinia } from '@pinia/testing';
import { dummyAppConfig } from '@/dummy/app-config';
import { expect } from 'vitest';
import { render } from '@/utils/test-utils';
import ConsoleHeader from '@/components/console-header/ConsoleHeader.vue';

describe('ConsoleHeader', () => {
  it('renders properly', () => {
    // when
    const { getByRole } = render(ConsoleHeader);

    // then
    expect(getByRole('img')).toHaveAttribute('alt', 'Hermes');
  });

  it('should display environment name', () => {
    // when
    const { getByText } = render(ConsoleHeader, {
      testPinia: createTestingPinia({
        initialState: {
          appConfig: {
            ...appConfigStoreState,
            appConfig: {
              ...dummyAppConfig,
              console: {
                ...dummyAppConfig.console,
                environmentName: 'TEST ENV',
              },
            },
          },
        },
      }),
    });

    // then
    expect(getByText('TEST ENV')).toBeVisible();
  });
});
