/* eslint-env node */
require('@rushstack/eslint-patch/modern-module-resolution');

module.exports = {
  env: {
    browser: true,
    node: true,
  },
  root: true,
  extends: [
    '@vue/eslint-config-prettier',
    '@vue/eslint-config-typescript',
    'eslint:recommended',
    'plugin:prettier/recommended',
    'plugin:vue/vue3-essential',
  ],
  globals: {
    afterAll: true,
    afterEach: true,
    assert: true,
    assertType: true,
    beforeAll: true,
    beforeEach: true,
    describe: true,
    expect: true,
    expectTypeOf: true,
    it: true,
    suite: true,
    test: true,
    vi: true,
    vitest: true,
  },
  parserOptions: {
    ecmaVersion: 'latest',
  },
  plugins: ['prettier', 'vue', 'sort-imports-es6-autofix'],
  rules: {
    'sort-imports-es6-autofix/sort-imports-es6': [
      'warn',
      {
        ignoreCase: true,
        ignoreMemberSort: false,
        memberSyntaxSortOrder: ['none', 'all', 'multiple', 'single'],
      },
    ],
    'no-unused-vars': 'off',
    '@typescript-eslint/no-unused-vars': ['error'],
  },
};
