import { expect } from 'vitest';
import { fireEvent } from '@testing-library/vue';
import { render } from '@/utils/test-utils';
import EnvironmentSwitch from '@/components/environment-switch/EnvironmentSwitch.vue';
import userEvent from '@testing-library/user-event';

const mockReplace = vi.fn();
const mockHref = vi.fn();
const mockOpen = vi.fn();

Object.defineProperty(window, 'location', {
  value: {
    get href() {
      return mockHref();
    },
    replace: mockReplace,
  },
});

window.open = mockOpen;

const TEST_URL_ENV_1 =
  'http://localhost:3000/ui/groups/pl.example.hermes/topics/pl.example.hermes.TemperatureChanged';
const TEST_URL_ENV_2 =
  'http://127.0.0.1:3000/ui/groups/pl.example.hermes/topics/pl.example.hermes.TemperatureChanged';

describe('EnvironmentSwitch', () => {
  it('should highlight the button for the selected environment', () => {
    // given
    mockHref.mockReturnValue(TEST_URL_ENV_1);

    // when
    const { getByText } = render(EnvironmentSwitch, {
      props: {
        knownEnvironments: [
          {
            name: 'env1',
            url: 'localhost:3000',
          },
          {
            name: 'env2',
            url: '127.0.0.1:3000',
          },
        ],
      },
    });

    // then
    expect(location.href).toBe(
      'http://localhost:3000/ui/groups/pl.example.hermes/topics/pl.example.hermes.TemperatureChanged',
    );
    expect(getByText('env1')).toBeVisible();
    expect(getByText('env2')).toBeVisible();
    expect(getByText('env1').closest('button')).toHaveClass('v-btn--active', {
      exact: false,
    });
    expect(getByText('env2').closest('button')).not.toHaveClass(
      'v-btn--active',
      { exact: false },
    );
  });

  it('should switch between urls without changing the rest of the path', async () => {
    // given
    mockHref.mockReturnValue(TEST_URL_ENV_1);

    // and
    const { getByText } = render(EnvironmentSwitch, {
      props: {
        knownEnvironments: [
          {
            name: 'env1',
            url: 'localhost:3000',
          },
          {
            name: 'env2',
            url: '127.0.0.1:3000',
          },
        ],
      },
    });

    // when
    await fireEvent.click(getByText('env2').closest('button'));

    // then
    expect(mockReplace).toHaveBeenCalledWith(TEST_URL_ENV_2);
  });

  it('should open the new tab on holding ctrl and clicking', async () => {
    // given
    mockHref.mockReturnValue(TEST_URL_ENV_1);

    // and
    const { getByText } = render(EnvironmentSwitch, {
      props: {
        knownEnvironments: [
          {
            name: 'env1',
            url: 'localhost:3000',
          },
          {
            name: 'env2',
            url: '127.0.0.1:3000',
          },
        ],
      },
    });

    // when
    const user = userEvent.setup();
    await user.keyboard('[ControlLeft>]');
    await user.click(getByText('env2').closest('button'));

    // then
    expect(mockOpen).toHaveBeenCalledWith(TEST_URL_ENV_2, '_blank');
  });

  it('should open the new tab on holding cmd and clicking', async () => {
    // given
    mockHref.mockReturnValue(TEST_URL_ENV_1);

    // and
    const { getByText } = render(EnvironmentSwitch, {
      props: {
        knownEnvironments: [
          {
            name: 'env1',
            url: 'localhost:3000',
          },
          {
            name: 'env2',
            url: '127.0.0.1:3000',
          },
        ],
      },
    });

    // when
    const user = userEvent.setup();
    await user.keyboard('[MetaLeft>]');
    await user.click(getByText('env2').closest('button'));

    // then
    expect(mockOpen).toHaveBeenCalledWith(TEST_URL_ENV_2, '_blank');
  });
});
