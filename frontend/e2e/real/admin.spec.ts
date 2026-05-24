import { expect, test } from '@playwright/test';
import {
  deleteEventByApi,
  findEventByTitle,
  futureDateTimeLocal,
  loginThroughUi,
  uniqueSuffix,
  waitForApi,
} from './real-api';

test.describe('real admin flow', () => {
  test.beforeEach(async ({ request }) => {
    await waitForApi(request);
  });

  test('creates a real event through the admin UI and publishes it to the catalog', async ({
    page,
    request,
  }) => {
    const title = `E2E Admin ${uniqueSuffix()}`;
    let createdEventId: number | undefined;

    try {
      await loginThroughUi(page, 'admin@treserve.com', 'admin123', /\/admin$/);

      await page.getByRole('link', { name: 'Добавить событие' }).click();
      await expect(page).toHaveURL(/\/admin\/create$/);

      await page.getByLabel('Название события').fill(title);
      await page.getByLabel('Описание').fill('Проверка полного E2E через реальный backend.');
      await page.getByLabel('Категория').fill('E2E');
      await page.getByLabel('Возрастное ограничение').fill('12+');
      await page.getByLabel('Длительность').fill('90');
      await page.getByLabel('Время проведения').fill(futureDateTimeLocal(45));

      const venueSelect = page.getByLabel('Место проведения');
      await expect(venueSelect.locator('option').first()).toBeVisible({ timeout: 15_000 });
      await venueSelect.selectOption({ index: 0 });

      await page.getByLabel('Цена билета').fill('777');

      await page.getByRole('button', { name: 'Сохранить событие' }).click();
      await expect(page).toHaveURL(/\/admin$/, { timeout: 15_000 });
      await expect(page.getByText(title)).toBeVisible({ timeout: 15_000 });

      const event = await findEventByTitle(request, title);
      expect(event?.id).toBeTruthy();
      createdEventId = event?.id;

      await page.goto('/events');
      await page.getByPlaceholder('Поиск').fill(title);
      await expect(page.getByText(title)).toBeVisible({ timeout: 15_000 });
    } finally {
      if (createdEventId) {
        await deleteEventByApi(request, createdEventId);
      }
    }
  });
});
