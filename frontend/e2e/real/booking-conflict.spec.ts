import { expect, test, type APIResponse } from '@playwright/test';
import {
  apiURL,
  authHeaders,
  createEventByApi,
  deleteEventByApi,
  getAvailableSeat,
  registerUserByApi,
  waitForApi,
  type AuthSession,
  type LockResponse,
} from './real-api';

test.describe('real backend booking conflicts', () => {
  test.beforeEach(async ({ request }) => {
    await waitForApi(request);
  });

  test('allows only one user to lock the same real seat under concurrent requests', async ({
    request,
  }) => {
    const { event, admin } = await createEventByApi(request);
    const createdLocks: Array<{ lock: LockResponse; auth: AuthSession }> = [];

    try {
      const [userOne, userTwo] = await Promise.all([
        registerUserByApi(request),
        registerUserByApi(request),
      ]);
      const seat = await getAvailableSeat(request, event.id);

      const attempts = await Promise.all(
        [userOne, userTwo].map(async (user) => ({
          auth: user.auth,
          response: await request.post(`${apiURL}/api/bookings/lock`, {
            headers: authHeaders(user.auth.token),
            data: {
              eventId: event.id,
              seatId: seat.seatId,
            },
          }),
        })),
      );

      const statuses = attempts.map(({ response }) => response.status()).sort((a, b) => a - b);
      expect(statuses).toEqual([200, 409]);

      const successfulAttempt = attempts.find(({ response }) => response.ok());
      expect(successfulAttempt).toBeDefined();

      const lock = (await (successfulAttempt!.response as APIResponse).json()) as LockResponse;
      createdLocks.push({ lock, auth: successfulAttempt!.auth });
    } finally {
      for (const { lock, auth } of createdLocks) {
        const cancel = await request.delete(`${apiURL}/api/bookings/${lock.lockId}`, {
          headers: authHeaders(auth.token),
        });
        expect.soft([204, 400, 404]).toContain(cancel.status());
      }

      await deleteEventByApi(request, event.id, admin.token);
    }
  });
});
