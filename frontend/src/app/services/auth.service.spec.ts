import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { AuthResponse } from '../models/user';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl;

  const mockResponse: AuthResponse = {
    token: 'access-token',
    refreshToken: 'refresh-token',
    user: {
      id: 1,
      email: 'test@test.com',
      name: 'Test',
      role: 'USER',
    },
  };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  //login
  it('should login and persist user + tokens', () => {
    service.login('test@test.com', '123456').subscribe(res => {
      expect(res.token).toBe('access-token');
    });

    const req = httpMock.expectOne(`${apiUrl}/api/auth/login`);
    expect(req.request.method).toBe('POST');

    req.flush(mockResponse);

    expect(localStorage.getItem('token')).toBe('access-token');
    expect(localStorage.getItem('refreshToken')).toBe('refresh-token');
    expect(localStorage.getItem('user')).toContain('test@test.com');
  });

  //register
  it('should register and persist user', () => {
    service.register('test@test.com', '123456', 'Test').subscribe();

    const req = httpMock.expectOne(`${apiUrl}/api/auth/register`);
    expect(req.request.method).toBe('POST');

    req.flush(mockResponse);

    expect(localStorage.getItem('token')).toBe('access-token');
  });

  //logout
  it('should clear storage on logout', () => {
    localStorage.setItem('token', 't');
    localStorage.setItem('refreshToken', 'r');

    service.logout();

    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(service.snapshot()).toBeNull();
  });

  //is logged in
  it('should return true if token exists', () => {
    localStorage.setItem('token', 't');

    expect(service.isLoggedIn()).toBeTrue();
  });

  it('should return false if token missing', () => {
    expect(service.isLoggedIn()).toBeFalse();
  });

  //snapshot
  it('should return current user snapshot', () => {
    service.login('a', 'b').subscribe();

    const req = httpMock.expectOne(`${apiUrl}/api/auth/login`);
    req.flush(mockResponse);

    expect(service.snapshot()?.email).toBe('test@test.com');
  });

  //refresh
  it('should refresh token and persist data', () => {
    localStorage.setItem('refreshToken', 'old-refresh');

    service.refresh().subscribe();

    const req = httpMock.expectOne(`${apiUrl}/api/auth/refresh`);
    expect(req.request.body).toEqual({ refreshToken: 'old-refresh' });

    req.flush(mockResponse);

    expect(localStorage.getItem('token')).toBe('access-token');
  });

  it('should throw error if refresh token missing', () => {
    expect(() => service.refresh()).toThrowError('No refresh token');
  });
});