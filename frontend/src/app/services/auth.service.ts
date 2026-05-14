import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthResponse, User } from '../models/user';
import { MockApiService } from './mock-api.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly currentUserSubject = new BehaviorSubject<User | null>(null);
  readonly currentUser$ = this.currentUserSubject.asObservable();

  constructor(private readonly mockApi: MockApiService) {
    const userJson = localStorage.getItem('user');

    if (userJson) {
      this.currentUserSubject.next(JSON.parse(userJson));
    }

  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.mockApi.login({ email, password }).pipe(tap((response) => this.persist(response)));
  }

  register(email: string, password: string, name: string): Observable<AuthResponse> {
    return this.mockApi.register({ email, password, name }).pipe(
      tap((response) => this.persist(response)),
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
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
}
