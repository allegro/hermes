import {
  appConfigStoreState,
  createTestingPiniaWithState,
} from '@/dummy/store';
import { createTestingPinia } from '@pinia/testing';
import { dummyAppConfig } from '@/dummy/app-config';
import { expect } from 'vitest';
import { render } from '@/utils/test-utils';
import ConfirmationDialog from '@/components/confirmation-dialog/ConfirmationDialog.vue';
import userEvent from '@testing-library/user-event';

describe('ConfirmationDialog', () => {
  const props = {
    actionButtonEnabled: true,
    title: 'Confirmation dialog',
    text: 'Dummy text',
    modelValue: true,
  };

  it('renders properly', () => {
    // when
    const { getByText, queryByText } = render(ConfirmationDialog, {
      testPinia: createTestingPiniaWithState(),
      props,
    });

    // then
    expect(getByText('Confirmation dialog')).toBeVisible();
    expect(
      queryByText('confirmationDialog.confirmText'),
    ).not.toBeInTheDocument();
    expect(getByText('Dummy text')).toBeVisible();
    expect(
      getByText('confirmationDialog.confirm').closest('button'),
    ).toBeEnabled();
    expect(
      getByText('confirmationDialog.cancel').closest('button'),
    ).toBeEnabled();
  });

  it('should disable action button', async () => {
    //given
    const props = {
      actionButtonEnabled: false,
      title: 'Confirmation dialog',
      text: 'Dummy text',
      modelValue: true,
    };

    // when
    const { getByText } = render(ConfirmationDialog, {
      testPinia: createTestingPiniaWithState(),
      props,
    });

    // then
    expect(
      getByText('confirmationDialog.confirm').closest('button'),
    ).toBeDisabled();
  });

  it('should require confirmation text', () => {
    // when
    const { getByText, getAllByText } = render(ConfirmationDialog, {
      testPinia: createTestingPinia({
        initialState: {
          appConfig: {
            ...appConfigStoreState,
            appConfig: {
              ...dummyAppConfig,
              console: {
                ...dummyAppConfig.console,
                criticalEnvironment: true,
              },
            },
          },
        },
      }),
      props,
    });

    // then
    expect(
      getAllByText('confirmationDialog.confirmText')[0],
    ).toBeInTheDocument();
    expect(
      getByText('confirmationDialog.confirm').closest('button'),
    ).toBeDisabled();
    expect(
      getByText('confirmationDialog.cancel').closest('button'),
    ).toBeEnabled();
  });

  it('should enable button when confirmation text match', async () => {
    // when
    const { getByText, getAllByText, getAllByRole } = render(
      ConfirmationDialog,
      {
        testPinia: createTestingPinia({
          initialState: {
            appConfig: {
              ...appConfigStoreState,
              appConfig: {
                ...dummyAppConfig,
                console: {
                  ...dummyAppConfig.console,
                  criticalEnvironment: true,
                },
              },
            },
          },
        }),
        props,
      },
    );

    // then
    expect(
      getAllByText('confirmationDialog.confirmText')[0],
    ).toBeInTheDocument();
    expect(
      getByText('confirmationDialog.confirm').closest('button'),
    ).toBeDisabled();
    expect(
      getByText('confirmationDialog.cancel').closest('button'),
    ).toBeEnabled();

    //when
    await userEvent.type(getAllByRole('textbox')[0], 'not matching text');

    //then
    expect(getAllByRole('textbox')[0]).toHaveValue('not matching text');
    expect(
      getByText('confirmationDialog.confirm').closest('button'),
    ).toBeDisabled();

    //when
    await userEvent.clear(getAllByRole('textbox')[0]);
    await userEvent.type(getAllByRole('textbox')[0], 'prod');

    //then
    expect(getAllByRole('textbox')[0]).toHaveValue('prod');
    expect(
      getByText('confirmationDialog.confirm').closest('button'),
    ).toBeEnabled();
  });
});
