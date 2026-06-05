import { FullConfig } from '@playwright/test';

async function globalSetup(_config: FullConfig) {
  const serverUrl = 'http://localhost:28080/health';
  const maxRetries = 30;
  const retryDelay = 1000;

  console.log('Checking backend server availability...');

  for (let i = 0; i < maxRetries; i++) {
    try {
      const response = await fetch(serverUrl);
      if (response.ok) {
        console.log('Backend server is ready!');
        return;
      }
    } catch {
      // Server not up yet
    }
    if (i < maxRetries - 1) {
      await new Promise((r) => setTimeout(r, retryDelay));
    }
  }

  console.warn(
    'WARNING: Backend server at localhost:28080 is not responding.\n' +
      'E2E tests requiring API calls will fail.\n' +
      'Start the server with: sbt sangeetServer/run',
  );
}

export default globalSetup;
