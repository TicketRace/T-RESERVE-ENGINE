import { APIRequestContext, APIResponse, Page, expect } from '@playwright/test';

export const apiURL = process.env['E2E_API_URL'] ?? 'http://localhost:8080';

export interface AuthSession {
  token: string;
  refreshToken: string;
  user: {
    id: number;
    email: string;
    name: string;
    role: 'USER' | 'ADMIN';
  };
}

export interface VenueResponse {
  id: number;
  name: string;
}

export interface EventResponse {
  id: number;
  title: string;
  description: string;
  startTime: string;
  basePrice: number;
  venueId?: number;
  venueName?: string;
}

export interface SeatResponse {
  seatId: number;
  seatLabel: string;
  rowLabel: string;
  seatNumber: number;
  status: 'AVAILABLE' | 'LOCKED' | 'BOOKED';
  price: number;
}

export interface LockResponse {
  lockId: number;
  expiresAt: string;
}

interface PageResponse<T> {
  content: T[];
}

export function uniqueSuffix(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export function authHeaders(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` };
}

export function futureIso(daysFromNow = 30): string {
  return new Date(Date.now() + daysFromNow * 24 * 60 * 60 * 1000).toISOString();
}

export function futureDateTimeLocal(daysFromNow = 30): string {
  return futureIso(daysFromNow).slice(0, 16);
}

export async function waitForApi(request: APIRequestContext): Promise<void> {
  for (let attempt = 1; attempt <= 60; attempt += 1) {
    try {
      const response = await request.get(`${apiURL}/api/events`, {
        params: { page: '0', size: '1' },
        timeout: 2_000,
      });
      if (response.ok()) return;
    } catch {
      // Backend or nginx may still be starting; retry below.
    }

    await new Promise((resolve) => setTimeout(resolve, 1_000));
  }

  throw new Error(
    `Backend API did not become reachable at ${apiURL}/api/events. Start the real backend or production stack before running e2e:real.`,
  );
}

async function parseJson<T>(response: APIResponse, label: string): Promise<T> {
  if (!response.ok()) {
    throw new Error(`${label} failed with ${response.status()}: ${await response.text()}`);
  }

  return (await response.json()) as T;
}

export async function loginByApi(
  request: APIRequestContext,
  email: string,
  password: string,
): Promise<AuthSession> {
  const response = await request.post(`${apiURL}/api/auth/login`, {
    data: { email, password },
  });

  return parseJson<AuthSession>(response, `Login as ${email}`);
}

export async function registerUserByApi(
  request: APIRequestContext,
  email = `e2e-user-${uniqueSuffix()}@example.com`,
  password = 'user123',
): Promise<{ email: string; password: string; auth: AuthSession }> {
  const response = await request.post(`${apiURL}/api/auth/register`, {
    data: {
      email,
      password,
      name: `E2E User ${uniqueSuffix()}`,
    },
  });

  return {
    email,
    password,
    auth: await parseJson<AuthSession>(response, `Register ${email}`),
  };
}

export async function loginThroughUi(
  page: Page,
  email: string,
  password: string,
  expectedPath: string | RegExp = '/',
): Promise<void> {
  await page.goto('/login');
  await page.getByPlaceholder('Email').fill(email);
  await page.getByPlaceholder('Пароль').fill(password);
  await page.getByRole('button', { name: 'Войти', exact: true }).click();
  await expect(page).toHaveURL(expectedPath, { timeout: 15_000 });
}

export async function getAdminSession(request: APIRequestContext): Promise<AuthSession> {
  return loginByApi(request, 'admin@treserve.com', 'admin123');
}

export async function getFirstVenue(
  request: APIRequestContext,
  adminToken: string,
): Promise<VenueResponse> {
  const response = await request.get(`${apiURL}/api/venues`, {
    headers: authHeaders(adminToken),
  });
  const venues = await parseJson<VenueResponse[]>(response, 'Load venues');

  if (venues.length === 0) {
    throw new Error('No venues were returned by /api/venues; real E2E needs seeded venues.');
  }

  return venues[0];
}

export async function createEventByApi(
  request: APIRequestContext,
  title = `E2E Full Stack ${uniqueSuffix()}`,
): Promise<{ event: EventResponse; admin: AuthSession }> {
  const admin = await getAdminSession(request);
  const venue = await getFirstVenue(request, admin.token);

  const response = await request.post(`${apiURL}/api/admin/events`, {
    headers: authHeaders(admin.token),
    data: {
      title,
      description: 'Created by Playwright real E2E against Spring Boot and PostgreSQL.',
      venueId: venue.id,
      startTime: futureIso(30),
      basePrice: 777,
      imageUrl: null,
      ageRestriction: '12+',
      category: 'E2E',
      durationMinutes: 90,
    },
  });

  return {
    event: await parseJson<EventResponse>(response, `Create event ${title}`),
    admin,
  };
}

export async function findEventByTitle(
  request: APIRequestContext,
  title: string,
): Promise<EventResponse | undefined> {
  const response = await request.get(`${apiURL}/api/events`, {
    params: {
      page: '0',
      size: '100',
      search: title,
    },
  });
  const page = await parseJson<PageResponse<EventResponse>>(response, `Find event ${title}`);
  return page.content.find((event) => event.title === title);
}

export async function deleteEventByApi(
  request: APIRequestContext,
  eventId: number,
  adminToken?: string,
): Promise<void> {
  const token = adminToken ?? (await getAdminSession(request)).token;
  const response = await request.delete(`${apiURL}/api/admin/events/${eventId}`, {
    headers: authHeaders(token),
  });

  if (![204, 404].includes(response.status())) {
    throw new Error(`Delete event ${eventId} failed with ${response.status()}: ${await response.text()}`);
  }
}

export async function getAvailableSeat(
  request: APIRequestContext,
  eventId: number,
): Promise<SeatResponse> {
  const response = await request.get(`${apiURL}/api/events/${eventId}/seats`);
  const seats = await parseJson<SeatResponse[]>(response, `Load seats for event ${eventId}`);
  const seat = seats.find((item) => item.status === 'AVAILABLE');

  if (!seat) {
    throw new Error(`No available seats for event ${eventId}`);
  }

  return seat;
}
