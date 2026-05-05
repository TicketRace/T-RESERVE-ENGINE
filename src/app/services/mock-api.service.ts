import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { Booking, LockResponse } from '../models/booking';
import { AdminEventSummary, EventItem, EventSession, VenueSeatTemplate } from '../models/event';
import { Seat } from '../models/seat';
import { AuthResponse, User } from '../models/user';
import { Venue } from '../models/venue';

interface SeatLock {
  lockId: number;
  sessionId: number;
  seatId: number;
  expiresAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class MockApiService {
  private currentUser: User | null = null;
  private lockIdCounter = 100;
  private bookings: Booking[] = [];
  private locks: SeatLock[] = [];
  private readonly venueTemplate: VenueSeatTemplate[] = this.createVenueTemplate();
  private events: EventItem[] = [
    {
      id: 1,
      title: 'Анна Каренина',
      description:
        'Тонкая сценическая версия романа с живой музыкой, глубоким светом и камерной драматургией.',
      venue: 'Дом Актера',
      imageUrl:
        'https://images.unsplash.com/photo-1507924538820-ede94a04019d?auto=format&fit=crop&w=1400&q=80',
      category: 'Спектакль',
      ageRating: '16+',
      tagline: 'Комедия, драма и большая сцена в одном вечере',
      featured: true,
      sessions: [
        { id: 101, eventId: 1, startsAt: '2026-01-15T16:00:00', price: 800 },
        { id: 102, eventId: 1, startsAt: '2026-01-15T19:00:00', price: 800 },
        { id: 103, eventId: 1, startsAt: '2026-01-20T16:00:00', price: 950 },
        { id: 104, eventId: 1, startsAt: '2026-01-20T19:00:00', price: 950 },
      ],
    },
    {
      id: 2,
      title: 'Мцыри',
      description: 'Пластический спектакль о свободе, пути и внутреннем выборе на темной сцене.',
      venue: 'Новая драма',
      imageUrl:
        'https://images.unsplash.com/photo-1518998053901-5348d3961a04?auto=format&fit=crop&w=1400&q=80',
      category: 'Спектакль',
      ageRating: '12+',
      tagline: 'Поэтическая история в современном сценическом языке',
      featured: false,
      sessions: [
        { id: 201, eventId: 2, startsAt: '2026-01-25T16:00:00', price: 700 },
        { id: 202, eventId: 2, startsAt: '2026-01-25T19:00:00', price: 700 },
      ],
    },
    {
      id: 3,
      title: 'Аватар Live',
      description:
        'Большой мультимедийный концерт с проекциями, электроникой и оркестровыми аранжировками.',
      venue: 'Городской концертный зал',
      imageUrl:
        'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=1400&q=80',
      category: 'Концерт',
      ageRating: '6+',
      tagline: 'Энергичный вечер с живым звуком и визуальным шоу',
      featured: false,
      sessions: [
        { id: 301, eventId: 3, startsAt: '2026-01-28T18:30:00', price: 1200 },
      ],
    },
    {
      id: 4,
      title: 'Концерт X',
      description: 'Инди-программа с акцентом на атмосферный свет и близкую посадку к сцене.',
      venue: 'Лофт Сцена',
      imageUrl:
        'https://images.unsplash.com/photo-1507874457470-272b3c8d8ee2?auto=format&fit=crop&w=1400&q=80',
      category: 'Концерт',
      ageRating: '18+',
      tagline: 'Камерный live-set с ночным городским настроением',
      featured: false,
      sessions: [
        { id: 401, eventId: 4, startsAt: '2026-02-02T20:00:00', price: 1100 },
      ],
    },
    {
      id: 5,
      title: 'Спектакль X',
      description:
        'Ироничная постановка с короткими актами, акцентом на актерскую игру и ритм диалогов.',
      venue: 'Малая сцена',
      imageUrl:
        'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=1400&q=80',
      category: 'Спектакль',
      ageRating: '16+',
      tagline: 'Современная сцена с плотной драматургией и быстрым темпом',
      featured: false,
      sessions: [
        { id: 501, eventId: 5, startsAt: '2026-02-05T19:00:00', price: 900 },
      ],
    },
    {
      id: 6,
      title: 'Концерт камерного оркестра',
      description:
        'Нежная акустическая программа с барочным настроением и мягкой посадкой света.',
      venue: 'Белый зал',
      imageUrl:
        'https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&w=1400&q=80',
      category: 'Концерт',
      ageRating: '6+',
      tagline: 'Тихий музыкальный вечер с красивой партитурой',
      featured: false,
      sessions: [
        { id: 601, eventId: 6, startsAt: '2026-02-12T18:00:00', price: 1000 },
      ],
    },
  ];
  private readonly seatsBySession: Record<number, Seat[]> = this.buildSeatsBySession();

  login(credentials: { email: string; password: string }): Observable<AuthResponse> {
    this.currentUser = {
      id: 1,
      email: credentials.email,
      name: credentials.email.includes('admin') ? 'Имя пользователя: X' : 'Имя пользователя: X',
      role: credentials.email.includes('admin') ? 'ADMIN' : 'USER',
    };

    return of(this.buildAuthResponse(this.currentUser)).pipe(delay(240));
  }

  register(userData: { email: string; password: string; name: string }): Observable<AuthResponse> {
    this.currentUser = {
      id: Date.now(),
      email: userData.email,
      name: userData.name,
      role: 'USER',
    };

    return of(this.buildAuthResponse(this.currentUser)).pipe(delay(260));
  }

  refresh(): Observable<AuthResponse> {
    const user =
      this.currentUser ??
      ({
        id: 1,
        email: 'user@test.com',
        name: 'Имя пользователя: X',
        role: 'USER',
      } satisfies User);

    this.currentUser = user;
    return of(this.buildAuthResponse(user)).pipe(delay(180));
  }

  getEvents(search = ''): Observable<EventItem[]> {
    const query = search.trim().toLowerCase();
    const data = !query
      ? this.events
      : this.events.filter(
          (event) =>
            event.title.toLowerCase().includes(query) ||
            event.category.toLowerCase().includes(query) ||
            event.venue.toLowerCase().includes(query),
        );

    return of(data.map((event) => ({ ...event, sessions: [...event.sessions] }))).pipe(delay(160));
  }

  getEventById(id: number): Observable<EventItem> {
    const event = this.events.find((item) => item.id === id);
    if (!event) {
      return throwError(() => new Error('Event not found'));
    }

    return of({ ...event, sessions: [...event.sessions] }).pipe(delay(140));
  }

  getSessions(eventId: number): Observable<EventSession[]> {
    const event = this.events.find((item) => item.id === eventId);
    return of(event ? [...event.sessions] : []).pipe(delay(140));
  }

  getSession(sessionId: number): Observable<EventSession> {
    const session = this.events.flatMap((event) => event.sessions).find((item) => item.id === sessionId);
    if (!session) {
      return throwError(() => new Error('Session not found'));
    }

    return of({ ...session }).pipe(delay(120));
  }

  getSeats(sessionId: number): Observable<Seat[]> {
    this.releaseExpiredLocks();
    this.simulateSeatActivity(sessionId);
    return of((this.seatsBySession[sessionId] ?? []).map((seat) => ({ ...seat }))).pipe(delay(120));
  }

  lockSeat(sessionId: number, seatId: number): Observable<LockResponse> {
    const seat = this.seatsBySession[sessionId]?.find((item) => item.seatId === seatId);
    if (!seat || seat.status !== 'AVAILABLE') {
      return throwError(() => new Error('Seat is not available'));
    }

    seat.status = 'LOCKED';
    this.lockIdCounter += 1;
    const expiresAt = new Date(Date.now() + 0.1 * 60 * 1000).toISOString(); //изменено на 6 сек
    this.locks.push({ lockId: this.lockIdCounter, sessionId, seatId, expiresAt });

    return of({ lockId: this.lockIdCounter, expiresAt }).pipe(delay(150));
  }

  confirmBooking(lockId: number, paymentMethod: string): Observable<string> {
    const lock = this.locks.find((item) => item.lockId === lockId);
    if (!lock) {
      return throwError(() => new Error('Lock not found'));
    }

    const session = this.events.flatMap((event) => event.sessions).find((item) => item.id === lock.sessionId);
    const event = this.events.find((item) => item.id === session?.eventId);
    const seat = this.seatsBySession[lock.sessionId]?.find((item) => item.seatId === lock.seatId);

    if (!session || !event || !seat) {
      return throwError(() => new Error('Booking payload is invalid'));
    }

    seat.status = 'BOOKED';
    this.bookings.unshift({
      ticketId: lock.lockId,
      event: { ...event, sessions: [...event.sessions] },
      session: { ...session },
      seat: seat.seatLabel,
      status: 'BOOKED',
      bookedAt: new Date().toISOString(),
      paymentMethod,
    });
    this.locks = this.locks.filter((item) => item.lockId !== lockId);

    return of('Booking confirmed').pipe(delay(180));
  }

  cancelBooking(lockId: number): Observable<void> {
    const lock = this.locks.find((item) => item.lockId === lockId);
    if (lock) {
      const seat = this.seatsBySession[lock.sessionId]?.find((item) => item.seatId === lock.seatId);
      if (seat?.status === 'LOCKED') {
        seat.status = 'AVAILABLE';
      }
    }

    this.locks = this.locks.filter((item) => item.lockId !== lockId);
    return of(void 0).pipe(delay(100));
  }

  getCurrentUser(): Observable<User> {
    const user =
      this.currentUser ??
      ({
        id: 1,
        email: 'user@test.com',
        name: 'Имя пользователя: X',
        role: 'USER',
      } satisfies User);

    return of(user).pipe(delay(100));
  }

  getUserBookings(): Observable<Booking[]> {
    return of(this.bookings.map((booking) => ({ ...booking, event: { ...booking.event }, session: { ...booking.session } }))).pipe(
      delay(140),
    );
  }

  getAdminEvents(): Observable<AdminEventSummary[]> {
    return of(
      this.events.map((event) => ({
        id: event.id,
        title: event.title,
        venue: event.venue,
        nextSession: event.sessions[0]?.startsAt ?? 'Дата не назначена',
      })),
    ).pipe(delay(120));
  }

  private venues: Venue[] = [
    { id: 1, name: 'Дом Актера', seatsCount: 28 },
    { id: 2, name: 'Новая драма', seatsCount: 28 },
    { id: 3, name: 'Городской концертный зал', seatsCount: 28 },
    { id: 4, name: 'Лофт Сцена', seatsCount: 28 },
    { id: 5, name: 'Малая сцена', seatsCount: 28 },
    { id: 6, name: 'Белый зал', seatsCount: 28 },
  ];

  getVenues(): Observable<any[]> {
    return of(this.venues.map(v => ({ ...v }))).pipe(delay(120));
  }

  getDashboard(): Observable<{
    totalEvents: number;
    totalBookings: number;
    revenue: number;
  }> {
    const revenue = this.bookings.reduce(
      (sum, b) => sum + b.session.price,
      0
    );

    return of({
      totalEvents: this.events.length,
      totalBookings: this.bookings.length,
      revenue,
    }).pipe(delay(150));
  }

  getVenueTemplate(): Observable<VenueSeatTemplate[]> {
    return of(this.venueTemplate.map((seat) => ({ ...seat }))).pipe(delay(100));
  }

  createEvent(payload: {
    title: string;
    description: string;
    venueId: number;
    startsAt: string;
    price: number;
  }): Observable<EventItem> {

    const venue = this.venues.find(v => v.id === payload.venueId);

    if (!venue) {
      return throwError(() => new Error('Venue not found'));
    }

    const id = Date.now();

    const newEvent: EventItem = {
      id,
      title: payload.title,
      description: payload.description,
      venue: venue.name,
      imageUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819',
      category: 'Новое событие',
      ageRating: '12+',
      tagline: 'Создано через admin API',
      featured: false,
      sessions: [
        {
          id: id * 10,
          eventId: id,
          startsAt: payload.startsAt,
          price: payload.price,
        }
      ]
    };

    this.events.unshift(newEvent);

    return of({ ...newEvent }).pipe(delay(200));
  }

  updateEvent(
    id: number,
    patch: Partial<EventItem> & { startsAt?: string }
  ): Observable<EventItem> {
    const index = this.events.findIndex(e => e.id === id);

    if (index === -1) {
      return throwError(() => new Error('Event not found'));
    }

    const existing = this.events[index];

    const updated: EventItem = {
      ...existing,
      title: patch.title ?? existing.title,
      sessions: existing.sessions.map(s =>
        s.id === existing.sessions[0].id
          ? {
              ...s,
              startsAt: patch.startsAt ?? s.startsAt,
            }
          : s
      ),
    };

    this.events[index] = updated;

    return of({ ...this.events[index] }).pipe(delay(150));
  }


  deleteEvent(id: number): Observable<void> {
    this.events = this.events.filter(e => e.id !== id);

    const sessionIds = this.events
      .flatMap(e => e.sessions)
      .map(s => s.id);

    Object.keys(this.seatsBySession).forEach(key => {
      if (!sessionIds.includes(+key)) {
        delete this.seatsBySession[+key];
      }
    });

    return of(void 0).pipe(delay(120));
  }

  private buildAuthResponse(user: User): AuthResponse {
    return {
      token: `mock-jwt-${Date.now()}`,
      refreshToken: `mock-refresh-${Date.now()}`,
      user,
    };
  }

  private buildSeatsBySession(): Record<number, Seat[]> {
    const result: Record<number, Seat[]> = {};
    const sessions = this.events.flatMap((event) => event.sessions);

    for (const session of sessions) {
      result[session.id] = this.createSeatGrid(session.price, session.id);
    }

    return result;
  }

  private createSeatGrid(basePrice: number, seed: number): Seat[] {
    const seats: Seat[] = [];
    let seatId = 1;

    for (let row = 1; row <= 4; row += 1) {
      for (let column = 1; column <= 7; column += 1) {
        const marker = (seed + row * 11 + column * 7) % 13;
        const status = marker === 1 ? 'BOOKED' : marker === 4 ? 'LOCKED' : 'AVAILABLE';
        seats.push({
          seatId,
          seatLabel: `Ряд ${row} место ${column}`,
          rowLabel: `${row}`,
          seatNumber: column,
          status,
          price: basePrice,
        });
        seatId += 1;
      }
    }

    return seats;
  }

  private createVenueTemplate(): VenueSeatTemplate[] {
    const seats: VenueSeatTemplate[] = [];
    let id = 1;

    for (let row = 1; row <= 4; row += 1) {
      for (let column = 1; column <= 7; column += 1) {
        seats.push({
          id,
          row,
          column,
          label: `${row}-${column}`,
          disabled: column === 1 || (row === 1 && column > 4),
        });
        id += 1;
      }
    }

    return seats;
  }

  private releaseExpiredLocks(): void {
    const now = Date.now();
    const expired = this.locks.filter((lock) => new Date(lock.expiresAt).getTime() <= now);

    for (const lock of expired) {
      const seat = this.seatsBySession[lock.sessionId]?.find((item) => item.seatId === lock.seatId);
      if (seat?.status === 'LOCKED') {
        seat.status = 'AVAILABLE';
      }
    }

    this.locks = this.locks.filter((lock) => new Date(lock.expiresAt).getTime() > now);
  }

  private simulateSeatActivity(sessionId: number): void {
    const seats = this.seatsBySession[sessionId];
    if (!seats?.length) {
      return;
    }

    const cursor = Math.floor(Date.now() / 3000) % seats.length;
    const seat = seats[cursor];
    const hasActiveLock = this.locks.some((lock) => lock.sessionId === sessionId && lock.seatId === seat.seatId);

    if (!hasActiveLock && seat.status === 'AVAILABLE' && cursor % 8 === 0) {
      seat.status = 'LOCKED';
    } else if (!hasActiveLock && seat.status === 'LOCKED' && cursor % 5 === 0) {
      seat.status = 'AVAILABLE';
    }
  }
}
