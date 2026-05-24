import { expect, test } from '@playwright/test';
import { mockApi } from './mocks';

test.describe('authentication', () => {
  test('logs in a user and opens the events catalog', async ({ page }) => {
    const api = await mockApi(page);

    await page.goto('/login');
    await page.getByPlaceholder('Email').fill('user@test.com');
    await page.getByPlaceholder('Пароль').fill('secret123');
    await page.getByRole('button', { name: 'Войти' }).click();

    await expect(page).toHaveURL(/\/events$/);
    await expect(page.getByText('Анна Каренина')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Личный кабинет' })).toBeVisible();
    await expect(page.getByText('Тестовый пользователь')).toBeVisible();
    expect(api.loginRequests).toEqual([{ email: 'user@test.com', password: 'secret123' }]);
  });

  test('shows a translated backend error for invalid credentials', async ({ page }) => {
    await mockApi(page, { loginStatus: 401 });

    await page.goto('/login');
    await page.getByPlaceholder('Email').fill('wrong@test.com');
    await page.getByPlaceholder('Пароль').fill('badpass');
    await page.getByRole('button', { name: 'Войти' }).click();

    await expect(page.getByText('Неверный email или пароль')).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });
});
