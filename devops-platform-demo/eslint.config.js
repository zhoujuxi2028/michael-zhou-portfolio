const js = require('@eslint/js');
const globals = require('globals');
const pluginCypress = require('eslint-plugin-cypress');

module.exports = [
  js.configs.recommended,
  pluginCypress.configs.recommended,
  {
    languageOptions: {
      globals: { ...globals.browser, ...globals.node },
      ecmaVersion: 'latest',
      sourceType: 'module',
    },
    rules: {
      'no-unused-vars': 'warn',
      'no-console': 'off',
      'cypress/no-unnecessary-waiting': 'warn',
      'no-restricted-properties': ['error', {
        object: 'Cypress',
        property: 'env',
        message: 'Cypress.env() was removed in Cypress 16. Use Cypress.expose() for non-sensitive values or cy.env() for secrets.',
      }],
    },
  },
  {
    ignores: ['node_modules/**'],
  },
];
