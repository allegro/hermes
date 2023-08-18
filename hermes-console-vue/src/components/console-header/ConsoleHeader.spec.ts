import {
  appConfigStoreState,
  authStoreState,
  createTestingPiniaWithState,
} from '@/dummy/store';
import { createTestingPinia } from '@pinia/testing';
import { dummyAppConfig } from '@/dummy/app-config';
import { expect } from 'vitest';
import { expiredToken, validToken } from '@/utils/jwt-utils';
import { render } from '@/utils/test-utils';
import ConsoleHeader from '@/components/console-header/ConsoleHeader.vue';

describe('ConsoleHeader', () => {
  it('renders properly', () => {
    // when
    const { getByRole } = render(ConsoleHeader, {
      testPinia: createTestingPiniaWithState(),
    });

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

  it('should display login button', () => {
    // when
    const { getByText } = render(ConsoleHeader, {
      testPinia: createTestingPinia({
        initialState: {
          appConfig: {
            ...appConfigStoreState,
            appConfig: {
              ...dummyAppConfig,
              auth: {
                ...dummyAppConfig.auth,
                oauth: {
                  ...dummyAppConfig.auth.oauth,
                  enabled: true,
                },
              },
            },
          },
          auth: {
            ...authStoreState,
          },
        },
      }),
    });

    // then
    expect(getByText('header.signIn')).toBeVisible();
  });

  it('should not display login button when auth is disabled', () => {
    // when
    const { queryByText } = render(ConsoleHeader, {
      testPinia: createTestingPinia({
        initialState: {
          appConfig: {
            ...appConfigStoreState,
            appConfig: {
              ...dummyAppConfig,
              auth: {
                ...dummyAppConfig.auth,
                oauth: {
                  ...dummyAppConfig.auth.oauth,
                  enabled: false,
                },
              },
            },
          },
          auth: {
            ...authStoreState,
          },
        },
      }),
    });

    // then
    expect(queryByText('header.signIn')).not.toBeInTheDocument();
    expect(queryByText('header.signout')).not.toBeInTheDocument();
  });

  it('should display logout button if token is valid', () => {
    // when
    const { getByText } = render(ConsoleHeader, {
      testPinia: createTestingPinia({
        initialState: {
          appConfig: {
            ...appConfigStoreState,
            appConfig: {
              ...dummyAppConfig,
              auth: {
                ...dummyAppConfig.auth,
                oauth: {
                  ...dummyAppConfig.auth.oauth,
                  enabled: true,
                },
              },
            },
          },
          auth: {
            ...authStoreState,
            accessToken: validToken,
          },
        },
      }),
    });

    // then
    expect(getByText('header.logout')).toBeVisible();
  });

  it('should display login button if token expired', () => {
    // when
    const { getByText } = render(ConsoleHeader, {
      testPinia: createTestingPinia({
        initialState: {
          appConfig: {
            ...appConfigStoreState,
            appConfig: {
              ...dummyAppConfig,
              auth: {
                ...dummyAppConfig.auth,
                oauth: {
                  ...dummyAppConfig.auth.oauth,
                  enabled: true,
                },
              },
            },
          },
          auth: {
            ...authStoreState,
            accessToken: expiredToken,
          },
        },
      }),
    });

    // then
    expect(getByText('header.signIn')).toBeVisible();
  });
});
