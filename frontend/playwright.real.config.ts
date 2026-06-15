import { defineConfig, devices } from '@playwright/test';

const port = process.env['PLAYWRIGHT_REAL_PORT'] ?? '4200';
const baseURL = process.env['E2E_BASE_URL'] ?? `http://localhost:${port}`;
const chromiumExecutablePath = process.env['PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH'];
const skipFrontendServer = process.env['E2E_SKIP_FRONTEND_SERVER'] === '1';

export default defineConfig({
  testDir: './e2e/real',
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: false,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  workers: 1,
  reporter: process.env['CI']
    ? [['github'], ['html', { open: 'never', outputFolder: 'playwright-report-real' }]]
    : [['list'], ['html', { open: 'never', outputFolder: 'playwright-report-real' }]],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  webServer: skipFrontendServer
    ? undefined
    : {
        command: `npm start -- --host localhost --port ${port}`,
        url: baseURL,
        reuseExistingServer: !process.env['CI'],
        timeout: 120_000,
      },
  projects: [
    {
      name: 'chromium-real',
      use: {
        ...devices['Desktop Chrome'],
        launchOptions: chromiumExecutablePath
          ? { executablePath: chromiumExecutablePath }
          : undefined,
      },
    },
  ],
});
