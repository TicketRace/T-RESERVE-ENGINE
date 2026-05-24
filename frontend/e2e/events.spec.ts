import { expect, test } from '@playwright/test';
import { mockApi } from './mocks';

test.describe('events catalog', () => {
  test('filters events by the search input', async ({ page }) => {
    await mockApi(page);

    await page.goto('/events');

    await expect(page.getByText('Анна Каренина')).toBeVisible();
    await expect(page.getByText('Мцыри')).toBeVisible();

    await page.getByPlaceholder('Поиск').fill('мцыри');

    await expect(page.getByText('Мцыри')).toBeVisible();
    await expect(page.getByText('Анна Каренина')).toHaveCount(0);
  });

  test('opens event details and starts seat selection', async ({ page }) => {
    await mockApi(page);

    await page.goto('/events');
    await page.getByText('Анна Каренина').click();

    await expect(page).toHaveURL(/\/event\/1$/);
    await expect(page.getByRole('heading', { name: 'Анна Каренина' })).toBeVisible();

    await page.getByRole('button', { name: 'Купить билеты' }).first().click();

    await expect(page).toHaveURL(/\/event\/1\/session\/1\/seats$/);
    await expect(page.getByRole('heading', { name: 'Выберите место' })).toBeVisible();
  });
});
