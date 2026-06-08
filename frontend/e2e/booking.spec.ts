import { expect, test } from '@playwright/test';
import { mockApi } from './mocks';

test.describe('booking flow', () => {
  test('selects an available seat, locks it and completes free payment', async ({ page }) => {
    const api = await mockApi(page);

    // Логиним пользователя, так как кнопка оплаты теперь проверяет авторизацию
    await page.goto('/');
    await page.evaluate(() => localStorage.setItem('token', 'fake-token'));

    await page.goto('/event/1/session/1/seats');

    await expect(page.getByRole('heading', { name: 'Выбор мест' })).toBeVisible();
    await expect(page.getByRole('button', { name: '1', exact: true })).toBeEnabled();
    await expect(page.getByRole('button', { name: '2', exact: true })).toBeDisabled();

    await page.getByRole('button', { name: '1', exact: true }).click();
    await expect(page.getByText(/Выбранное место:\s*A-1/)).toBeVisible();

    await page.getByRole('button', { name: 'Перейти к оплате' }).click();

    await expect(page).toHaveURL(/\/payment\/1$/);
    await expect(page.getByRole('heading', { name: 'Ваш заказ' })).toBeVisible();
    await expect(page.getByText(/минут на завершение заказа/)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Подтвердить и оплатить' })).toBeEnabled();

    await page.getByRole('button', { name: 'Подтвердить и оплатить' }).click();

    await expect(page).toHaveURL(/\/payment-success$/);
    await expect(page.getByText('Билеты оплачены. Их можно посмотреть в личном кабинете')).toBeVisible();
    expect(api.lockRequests).toEqual([{ eventId: 1, seatId: 101 }]);
    expect(api.confirmLockIds).toEqual([9001]);
  });

  test('shows an error when the selected seat cannot be locked', async ({ page }) => {
    await mockApi(page, { lockStatus: 409 });

    await page.goto('/');
    await page.evaluate(() => localStorage.setItem('token', 'fake-token'));

    await page.goto('/event/1/session/1/seats');
    await page.getByRole('button', { name: '1', exact: true }).click();
    await page.getByRole('button', { name: 'Перейти к оплате' }).click();

    await expect(page).toHaveURL(/\/payment\/1$/);
    await expect(page.getByText('Место уже занято')).toBeVisible();
  });
});
