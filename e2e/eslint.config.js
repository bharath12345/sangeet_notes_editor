const tseslint = require('typescript-eslint');

module.exports = [
  {
    ignores: ['node_modules/', 'dist/', 'playwright-report/', 'test-results/', 'eslint.config.js'],
  },
  ...tseslint.configs.recommended,
  {
    files: ['**/*.ts'],
    rules: {
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
      '@typescript-eslint/explicit-function-return-type': 'off',
      '@typescript-eslint/no-explicit-any': 'warn',
    },
  },
];
