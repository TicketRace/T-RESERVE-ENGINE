import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthResponse, User } from '../models/user';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly currentUserSubject = new BehaviorSubject<User | null>(null);
  readonly currentUser$ = this.currentUserSubject.asObservable();
  private apiUrl = environment.apiUrl;
  
 constructor(private http: HttpClient) {
  const userJson = localStorage.getItem('user');

  if (userJson) {
    this.currentUserSubject.next(JSON.parse(userJson));
  }
 }
 
 login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/api/auth/login`,
      { email, password }
    ).pipe(tap(res => this.persist(res)));
  }

 register(email: string, password: string, name: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/api/auth/register`,
      { email, password, name }
    ).pipe(tap(res => this.persist(res)));
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    this.currentUserSubject.next(null);
  }

  isLoggedIn(): boolean {
    return Boolean(localStorage.getItem('token'));
  }

  snapshot(): User | null {
    return this.currentUserSubject.value;
  }

  private persist(response: AuthResponse): void {
    localStorage.setItem('token', response.token);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('user', JSON.stringify(response.user));
    this.currentUserSubject.next(response.user);
  }

  refresh(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
    throw new Error('No refresh token');
    }
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/api/auth/refresh`,
      { refreshToken }
    ).pipe(
      tap(res => this.persist(res))
    );
  }

  /** Редирект на Google OAuth2 — бэкенд обрабатывает весь flow */
  googleLogin(): void {
    window.location.href = `${this.apiUrl}/oauth2/authorization/google`;
  }

  /**
   * Вызывается из OAuth2CallbackComponent после редиректа от бэкенда.
   * Декодирует JWT для получения user info и сохраняет токены.
   */
  handleOAuth2Callback(token: string, refreshToken: string): void {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const user: User = {
        id:    Number(payload.sub),
        email: payload.email,
        name:  payload.name || payload.email.split('@')[0],
        role:  payload.role as 'USER' | 'ADMIN',
      };
      this.persist({ token, refreshToken, user });
    } catch {
      console.error('Failed to parse OAuth2 JWT');
    }
  }
}
