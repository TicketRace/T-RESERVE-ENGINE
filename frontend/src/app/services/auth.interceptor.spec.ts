import { authInterceptor } from './auth.interceptor';
import { HttpRequest } from '@angular/common/http';

describe('authInterceptor', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('should add Authorization header when token exists', () => {
    localStorage.setItem('token', 'abc-token');

    const req = new HttpRequest('GET', '/test');
    let capturedRequest: HttpRequest<any> | null = null;
    
    const next = (r: HttpRequest<any>) => {
      capturedRequest = r;
      return null as any;
    };

    authInterceptor(req, next);

    expect(capturedRequest).not.toBeNull();
    expect(capturedRequest!.headers.get('Authorization')).toBe('Bearer abc-token');
  });

  it('should NOT add Authorization header when token missing', () => {
    const req = new HttpRequest('GET', '/test');
    let capturedRequest: HttpRequest<any> | null = null;
    
    const next = (r: HttpRequest<any>) => {
      capturedRequest = r;
      return null as any;
    };

    authInterceptor(req, next);

    expect(capturedRequest).not.toBeNull();
    expect(capturedRequest!.headers.has('Authorization')).toBeFalse();
  });
});