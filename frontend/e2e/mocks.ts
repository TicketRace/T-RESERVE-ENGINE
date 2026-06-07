import { Page, Route } from '@playwright/test';

type HttpMethod = 'GET' | 'POST' | 'DELETE' | 'PATCH' | 'PUT';

interface MockOptions {
  loginStatus?: 200 | 401;
  lockStatus?: 200 | 409;
}

export interface ApiMockState {
  loginRequests: unknown[];
  lockRequests: unknown[];
  confirmLockIds: number[];
}

const event = {
  id: 1,
  title: 'Анна Каренина',
  description: 'Камерная постановка классического романа на большой сцене.',
  venue: {
    id: 10,
    name: 'Дом Актера',
    address: 'Театральная площадь, 1',
  },
  imageUrl: null,
  category: 'Спектакль',
  ageRestriction: '16+',
  startTime: '2026-05-12T19:00:00',
  basePrice: 800,
};

const secondEvent = {
  id: 2,
  title: 'Мцыри',
  description: 'Пластический спектакль о свободе и выборе.',
  venue: {
    id: 11,
    name: 'Новая драма',
    address: 'ул. Свободы, 7',
  },
  imageUrl: null,
  category: 'Спектакль',
  ageRestriction: '12+',
  startTime: '2026-05-13T20:00:00',
  basePrice: 700,
};

const seats = [
  {
    seatId: 101,
    seatLabel: 'A-1',
    rowLabel: 'A',
    seatNumber: 1,
    status: 'AVAILABLE',
    price: 800,
  },
  {
    seatId: 102,
    seatLabel: 'A-2',
    rowLabel: 'A',
    seatNumber: 2,
    status: 'BOOKED',
    price: 800,
  },
  {
    seatId: 103,
    seatLabel: 'A-3',
    rowLabel: 'A',
    seatNumber: 3,
    status: 'LOCKED',
    price: 800,
  },
];

function json(route: Route, status: number, body: unknown) {
  return route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });
}

function text(route: Route, status: number, body: string) {
  return route.fulfill({
    status,
    contentType: 'text/plain',
    body,
  });
}

async function postData(route: Route): Promise<unknown> {
  const raw = route.request().postData();
  if (!raw) return null;

  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
}

export async function mockApi(page: Page, options: MockOptions = {}): Promise<ApiMockState> {
  const state: ApiMockState = {
    loginRequests: [],
    lockRequests: [],
    confirmLockIds: [],
  };

  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const method = request.method() as HttpMethod;
    const url = new URL(request.url());
    const path = url.pathname;

    if (method === 'POST' && path === '/api/auth/login') {
      state.loginRequests.push(await postData(route));

      if (options.loginStatus === 401) {
        return json(route, 401, { message: 'Invalid email or password' });
      }

      return json(route, 200, {
        token: 'access-token-for-e2e',
        refreshToken: 'refresh-token-for-e2e',
        user: {
          id: 1,
          email: 'user@test.com',
          name: 'Тестовый пользователь',
          role: 'USER',
        },
      });
    }

    if (method === 'POST' && path === '/api/auth/register') {
      return json(route, 201, {
        token: 'new-user-access-token',
        refreshToken: 'new-user-refresh-token',
        user: {
          id: 2,
          email: 'new-user@test.com',
          name: 'Новый пользователь',
          role: 'USER',
        },
      });
    }

    if (method === 'GET' && path === '/api/events') {
      return json(route, 200, {
        content: [event, secondEvent],
        totalElements: 2,
        totalPages: 1,
        number: 0,
        size: 20,
      });
    }

    if (method === 'GET' && path === '/api/events/1') {
      return json(route, 200, event);
    }

    if (method === 'GET' && path === '/api/events/2') {
      return json(route, 200, secondEvent);
    }

    if (method === 'GET' && path === '/api/events/1/seats') {
      return json(route, 200, seats);
    }

    if (method === 'POST' && path === '/api/bookings/lock') {
      state.lockRequests.push(await postData(route));

      if (options.lockStatus === 409) {
        return json(route, 409, { message: 'Seat is already locked or booked' });
      }

      return json(route, 200, {
        lockId: 9001,
        expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
      });
    }

    const confirmMatch = path.match(/^\/api\/bookings\/(\d+)\/confirm$/);
    if (method === 'POST' && confirmMatch) {
      state.confirmLockIds.push(Number(confirmMatch[1]));
      return text(route, 200, 'OK');
    }

    if (method === 'GET' && path === '/api/users/me') {
      return json(route, 200, {
        id: 1,
        email: 'user@test.com',
        name: 'Тестовый пользователь',
        role: 'USER',
      });
    }

    if (method === 'GET' && path === '/api/users/me/bookings') {
      return json(route, 200, [
        {
          ticketId: 9001,
          eventTitle: event.title,
          seatLabel: 'A-1',
          status: 'BOOKED',
          price: 800,
          bookedAt: '2026-05-12T19:05:00',
          eventStartTime: event.startTime,
        },
      ]);
    }

    return json(route, 404, { message: `No E2E mock for ${method} ${path}` });
  });

  return state;
}
