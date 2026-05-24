import { expect, test } from '@playwright/test';
import {
  apiURL,
  authHeaders,
  createEventByApi,
  deleteEventByApi,
  getAvailableSeat,
  registerUserByApi,
  waitForApi,
  type LockResponse,
} from './real-api';

test.describe('real backend booking conflicts', () => {
  test.beforeEach(async ({ request }) => {
    await waitForApi(request);
  });

  test('allows only one user to lock the same real seat', async ({ request }) => {
    const { event, admin } = await createEventByApi(request);
    const userOne = await registerUserByApi(request);
    const userTwo = await registerUserByApi(request);
    const seat = await getAvailableSeat(request, event.id);

    const firstLock = await request.post(`${apiURL}/api/bookings/lock`, {
      headers: authHeaders(userOne.auth.token),
      data: {
        eventId: event.id,
        seatId: seat.seatId,
      },
    });
    await expect(firstLock).toBeOK();
    const firstLockBody = (await firstLock.json()) as LockResponse;

    const secondLock = await request.post(`${apiURL}/api/bookings/lock`, {
      headers: authHeaders(userTwo.auth.token),
      data: {
        eventId: event.id,
        seatId: seat.seatId,
      },
    });
    expect(secondLock.status()).toBe(409);

    const cancel = await request.delete(`${apiURL}/api/bookings/${firstLockBody.lockId}`, {
      headers: authHeaders(userOne.auth.token),
    });
    expect([204, 400, 404]).toContain(cancel.status());

    await deleteEventByApi(request, event.id, admin.token);
  });
});
