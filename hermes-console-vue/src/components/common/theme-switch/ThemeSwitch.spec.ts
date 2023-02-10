import { createVuetify } from 'vuetify';
import { fireEvent } from '@testing-library/vue';
import { vuetifyRender } from '@/utils/test-utils';
import ThemeSwitch from '@/components/common/theme-switch/ThemeSwitch.vue';

describe('ThemeSwitch', () => {
  const vuetify = createVuetify();

  beforeEach(() => {
    vuetify.theme.global.name.value = 'light';
    window.localStorage.clear();
  });

  it('should render theme switch button', () => {
    // when
    const { getByRole } = vuetifyRender(ThemeSwitch, {}, vuetify);
    // then
    expect(getByRole('button')).toBeTruthy();
  });

  it.each([
    { initialTheme: 'light', changedTheme: 'dark' },
    { initialTheme: 'dark', changedTheme: 'light' },
  ])(
    'should change Vuetify theme on button click (initial theme: %s)',
    ({ initialTheme, changedTheme }) => {
      // given
      const { getByRole } = vuetifyRender(ThemeSwitch, {}, vuetify);
      vuetify.theme.global.name.value = initialTheme;

      // when
      fireEvent.click(getByRole('button'));

      // then
      expect(vuetify.theme.global.name.value).toBe(changedTheme);
    },
  );

  it('should persist theme preference in local storage', () => {
    // given
    const { getByRole } = vuetifyRender(ThemeSwitch, {}, vuetify);
    vuetify.theme.global.name.value = 'light';

    // when
    fireEvent.click(getByRole('button'));

    // then
    expect(localStorage.getItem('hermes-console-theme')).toBe('dark');
  });

  it.each([
    { persistedTheme: 'light', loadedTheme: 'light' },
    { persistedTheme: 'dark', loadedTheme: 'dark' },
  ])(
    'should load user persisted "%s" theme preference from local storage',
    ({ persistedTheme, loadedTheme }) => {
      // given
      localStorage.setItem('hermes-console-theme', persistedTheme);

      // when
      vuetifyRender(ThemeSwitch, {}, vuetify);

      // then
      expect(vuetify.theme.global.name.value).toBe(loadedTheme);
    },
  );

  it('should set light theme if there is no entry in local storage', () => {
    // when
    vuetifyRender(ThemeSwitch, {}, vuetify);

    // then
    expect(vuetify.theme.global.name.value).toBe('light');
  });
});
