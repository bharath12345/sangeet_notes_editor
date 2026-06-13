import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: '.',
  testMatch: ['tests/**/*.spec.ts', 'integration/**/*.spec.ts'],
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: process.env.CI ? 'github' : 'html',
  timeout: 30000,

  use: {
    baseURL: 'http://localhost:28082',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  webServer: {
    command: 'python3 -m http.server 28082 --directory ../sangeet-web/public',
    port: 28082,
    reuseExistingServer: !process.env.CI,
    timeout: 10000,
  },
});
