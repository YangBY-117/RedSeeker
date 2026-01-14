const { chromium } = require('playwright');

const FRONTEND_URL = process.env.FRONTEND_URL || 'http://localhost:5173/';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();

  await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
  await page.evaluate(() => window.dispatchEvent(new CustomEvent('auth:unauthorized')));
  await page.waitForSelector('.login-modal', { timeout: 10000 });
  await page.click('.login-form .modal-footer a');
  await page.waitForSelector('.register-form', { timeout: 10000 });

  const inputs = await page.$$('.register-form input.form-input');
  const username = 'codex_reg_' + Date.now();
  const password = 'Passw0rd!123';

  await inputs[0].fill(username);
  await inputs[1].fill(password);
  await inputs[2].fill(password);
  await page.click('.register-form .submit-btn');
  await page.waitForTimeout(2000);

  const error = await page.$('.register-form .error-message');
  const errorText = error ? (await error.textContent()) : '';

  console.log(JSON.stringify({ username, errorText: (errorText || '').trim() }));
  await browser.close();
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
