/* eslint-env node */
require('@rushstack/eslint-patch/modern-module-resolution');

module.exports = {
  root: true,
  extends: [
    '@vue/eslint-config-prettier',
    '@vue/eslint-config-typescript',
    'eslint:recommended',
    'plugin:prettier/recommended',
    'plugin:vue/vue3-essential',
  ],
  parserOptions: {
    ecmaVersion: 'latest',
  },
  plugins: [
    'prettier',
    'vue',
    'sort-imports-es6-autofix',
  ],
  rules: {
    'sort-imports-es6-autofix/sort-imports-es6': ['warn', {
      'ignoreCase': true,
      'ignoreMemberSort': false,
      'memberSyntaxSortOrder': ['none', 'all', 'multiple', 'single'],
    }],
  },
};
