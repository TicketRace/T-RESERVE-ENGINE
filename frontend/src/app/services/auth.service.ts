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
}
