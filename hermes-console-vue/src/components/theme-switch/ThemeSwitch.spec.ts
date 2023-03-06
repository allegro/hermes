import { createVuetify } from 'vuetify';
import { fireEvent } from '@testing-library/vue';
import { render } from '@/utils/test-utils';
import ThemeSwitch from '@/components/theme-switch/ThemeSwitch.vue';

describe('ThemeSwitch', () => {
  const vuetify = createVuetify();

  beforeEach(() => {
    vuetify.theme.global.name.value = 'light';
    window.localStorage.clear();
  });

  it('should render theme switch button', () => {
    // when
    const { getByRole } = render(ThemeSwitch, {}, vuetify);

    // then
    expect(getByRole('button')).toBeInTheDocument();
  });

  it.each([
    ['light', 'dark'],
    ['dark', 'light'],
  ])(
    'should change Vuetify theme on button click (initial theme: %s)',
    async (initialTheme: string, changedTheme: string) => {
      // given
      const { getByRole } = render(ThemeSwitch, {}, vuetify);
      vuetify.theme.global.name.value = initialTheme;

      // when
      await fireEvent.click(getByRole('button'));

      // then
      expect(vuetify.theme.global.name.value).toBe(changedTheme);
    },
  );

  it('should persist theme preference in local storage', async () => {
    // given
    const { getByRole } = render(ThemeSwitch, {}, vuetify);
    vuetify.theme.global.name.value = 'light';

    // when
    await fireEvent.click(getByRole('button'));

    // then
    expect(localStorage.getItem('hermes-console-theme')).toBe('dark');
  });

  it.each([
    ['light', 'light'],
    ['dark', 'dark'],
  ])(
    'should load user persisted "%s" theme preference from local storage',
    (persistedTheme: string, loadedTheme: string) => {
      // given
      localStorage.setItem('hermes-console-theme', persistedTheme);

      // when
      render(ThemeSwitch, {}, vuetify);

      // then
      expect(vuetify.theme.global.name.value).toBe(loadedTheme);
    },
  );

  it('should set light theme if there is no entry in local storage', () => {
    // when
    render(ThemeSwitch, {}, vuetify);

    // then
    expect(vuetify.theme.global.name.value).toBe('light');
  });
});
