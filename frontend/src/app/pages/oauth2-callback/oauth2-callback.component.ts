import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

/**
 * Обрабатывает redirect от бэкенда после Google OAuth2.
 * URL: /oauth2/callback#token=...&refreshToken=...
 *
 * Используем fragment (#) вместо query params (?):
 * fragment не отправляется на сервер и не попадает в logs.
 */
@Component({
  selector: 'app-oauth2-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="callback-page">
      <div class="spinner"></div>
      <p>{{ error || 'Авторизация через Google...' }}</p>
    </div>
  `,
  styles: [`
    .callback-page {
      display: grid;
      place-items: center;
      min-height: 100vh;
      gap: 16px;
      font-family: inherit;
      color: #666;
    }
    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid #e6e6e6;
      border-top-color: #785aff;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class OAuth2CallbackComponent implements OnInit {
  error = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly authService: AuthService,
  ) {}

  ngOnInit(): void {
    // Читаем токены из URL fragment (#token=...&refreshToken=...)
    this.route.fragment.subscribe(fragment => {
      const params = new URLSearchParams(fragment ?? '');
      const token        = params.get('token');
      const refreshToken = params.get('refreshToken');
      const error        = params.get('error');

      if (error) {
        this.error = 'Ошибка авторизации через Google';
        setTimeout(() => this.router.navigate(['/login'], {
          queryParams: { error: 'oauth2' }
        }), 2000);
        return;
      }

      if (token && refreshToken) {
        this.authService.handleOAuth2Callback(token, refreshToken);
        const user = this.authService.snapshot();
        if (user?.role === 'ADMIN') {
          this.router.navigate(['/admin']);
        } else {
          this.router.navigate(['/events']);
        }
      } else {
        this.router.navigate(['/login']);
      }
    });
  }
}

