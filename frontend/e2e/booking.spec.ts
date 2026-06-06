import { expect, test } from '@playwright/test';
import { mockApi } from './mocks';

test.describe('booking flow', () => {
  test('selects an available seat, locks it and completes free payment', async ({ page }) => {
    const api = await mockApi(page);

    await page.goto('/event/1/session/1/seats');

    await expect(page.getByRole('heading', { name: 'Выберите место' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Место A-1', exact: true })).toBeEnabled();
    await expect(page.getByRole('button', { name: 'Место A-2', exact: true })).toBeDisabled();

    await page.getByRole('button', { name: 'Место A-1', exact: true }).click();
    await expect(page.getByText(/Выбрано мест:\s*1\. A-1/)).toBeVisible();

    await page.getByRole('button', { name: 'оплатить' }).click();

    await expect(page).toHaveURL(/\/payment\/1$/);
    await expect(page.getByRole('heading', { name: 'Оплата выбранных билетов' })).toBeVisible();
    await expect(page.getByText(/Осталось:/)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Купить бесплатно' })).toBeEnabled();

    await page.getByRole('button', { name: 'Купить бесплатно' }).click();

    await expect(page).toHaveURL(/\/payment-success$/);
    await expect(page.getByText('Билеты оплачены. Их можно посмотреть в личном кабинете')).toBeVisible();
    expect(api.lockRequests).toEqual([{ eventId: 1, seatId: 101 }]);
    expect(api.confirmLockIds).toEqual([9001]);
  });

  test('shows an error when the selected seat cannot be locked', async ({ page }) => {
    await mockApi(page, { lockStatus: 409 });

    await page.goto('/event/1/session/1/seats');
    await page.getByRole('button', { name: 'Место A-1', exact: true }).click();
    await page.getByRole('button', { name: 'оплатить' }).click();

    await expect(page).toHaveURL(/\/payment\/1$/);
    await expect(page.getByText('Место уже занято')).toBeVisible();
  });
});
