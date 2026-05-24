import { expect, test } from '@playwright/test';
import { createEventByApi, deleteEventByApi, loginThroughUi, registerUserByApi, waitForApi } from './real-api';

test.describe('real full-stack booking flow', () => {
  test.beforeEach(async ({ request }) => {
    await waitForApi(request);
  });

  test('registers a real user, books a real seat and shows the ticket in profile', async ({
    page,
    request,
  }) => {
    const { event, admin } = await createEventByApi(request);
    let bookingCompleted = false;

    try {
      const user = await registerUserByApi(request);

      await loginThroughUi(page, user.email, user.password);

      await page.goto('/events');
      await page.getByPlaceholder('Поиск').fill(event.title);
      await expect(page.getByText(event.title)).toBeVisible({ timeout: 15_000 });

      await page.getByText(event.title).click();
      await expect(page).toHaveURL(new RegExp(`/event/${event.id}$`));
      await expect(page.getByRole('heading', { name: event.title })).toBeVisible();
      await expect(page.getByText(/777/)).toBeVisible();

      await page.getByRole('button', { name: 'Купить билеты' }).first().click();
      await expect(page).toHaveURL(new RegExp(`/event/${event.id}/session/1/seats$`));
      await expect(page.getByRole('heading', { name: 'Выберите место' })).toBeVisible();

      const firstSeat = page.getByRole('button', { name: 'Место A-1', exact: true });
      await expect(firstSeat).toBeEnabled({ timeout: 15_000 });
      await firstSeat.click();
      await expect(page.getByText(/Выбрано мест:\s*1\. A-1/)).toBeVisible();

      await page.getByRole('button', { name: 'оплатить' }).click();
      await expect(page).toHaveURL(new RegExp(`/payment/${event.id}$`));
      await expect(page.getByRole('heading', { name: 'Оплата выбранных билетов' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Купить бесплатно' })).toBeEnabled({
        timeout: 15_000,
      });

      await page.getByRole('button', { name: 'Купить бесплатно' }).click();
      bookingCompleted = true;
      await expect(page).toHaveURL(/\/payment-success$/);
      await expect(
        page.getByText('Билеты оплачены. Их можно посмотреть в личном кабинете'),
      ).toBeVisible();

      await page.getByRole('link', { name: 'Перейти в личный кабинет' }).click();
      await expect(page).toHaveURL(/\/profile$/);
      await expect(page.getByText(event.title)).toBeVisible({ timeout: 15_000 });
      await expect(page.getByText(/Дата покупки:/)).toBeVisible();
    } finally {
      // After confirmation the public admin API intentionally rejects deleting events with BOOKED tickets.
      // The CI job runs this suite against a disposable Docker volume and removes it with `docker compose down -v`.
      if (!bookingCompleted) {
        await deleteEventByApi(request, event.id, admin.token);
      }
    }
  });
});
