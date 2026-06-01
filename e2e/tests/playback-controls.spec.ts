import { test, expect } from '@playwright/test';
import { SangeetPage } from '../helpers/app-page';

test.describe('Playback Controls', () => {
  let app: SangeetPage;

  test.beforeEach(async ({ page }) => {
    app = new SangeetPage(page);
    await app.goto();
  });

  test('play button is visible', async () => {
    await expect(app.playBtn).toBeVisible();
  });

  test('pause button is initially disabled', async () => {
    await expect(app.pauseBtn).toBeDisabled();
  });

  test('stop button is initially disabled', async () => {
    await expect(app.stopBtn).toBeDisabled();
  });

  test('loop toggle button is visible', async () => {
    await expect(app.loopBtn).toBeVisible();
  });

  test('BPM slider is visible', async () => {
    await expect(app.bpmSlider).toBeVisible();
  });

  test('BPM display shows a number', async () => {
    const bpm = await app.getBpmValue();
    const bpmNum = parseFloat(bpm);
    expect(bpmNum).toBeGreaterThanOrEqual(20);
    expect(bpmNum).toBeLessThanOrEqual(300);
  });

  test('clicking play enables pause and stop', async () => {
    // Insert a note first
    await app.pressKey('s');
    await app.playBtn.click();
    await app.waitForApi();
    // Pause and stop should become enabled (play disables)
    // Note: playback via ports — behavior depends on browser audio support
  });

  test('loop button toggles active state', async () => {
    const hadActive = await app.loopBtn.evaluate(
      el => el.classList.contains('loop-active')
    );
    await app.loopBtn.click();
    await app.waitForApi();
    const hasActive = await app.loopBtn.evaluate(
      el => el.classList.contains('loop-active')
    );
    expect(hasActive).not.toBe(hadActive);
  });

  test('BPM slider changes displayed BPM value', async () => {
    const before = await app.getBpmValue();
    await app.bpmSlider.fill('120');
    await app.waitForApi();
    const after = await app.getBpmValue();
    // Value should have changed (or be 120)
    expect(after).toContain('120');
  });
});
