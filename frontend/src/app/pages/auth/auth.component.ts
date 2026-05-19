import { Component } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css',
})
export class AuthComponent {
  form!: ReturnType<FormBuilder['group']>;
  mode: 'login' | 'register' = 'login';

  error = '';
  hidePassword = true;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      password: [
        '',
        [Validators.required, Validators.minLength(6), Validators.maxLength(100)],
      ],
    });

    this.route.url.subscribe(url => {
      this.mode = url[0].path === 'register' ? 'register' : 'login';
      this.error = '';
      this.form.reset();
    });
  }

  private extractBackendMessage(err: HttpErrorResponse): string {
    const e = err.error;

    if (!e) return '';

    if (typeof e === 'string') return e;

    return e.message || e.error || '';
  }

  private translateError(message: string): string {
    if (!message) return '';

    if (message.includes('Invalid email or password')) {
      return 'Неверный email или пароль';
    }

    if (message.includes('Email already registered')) {
      return 'Пользователь уже существует';
    }

    if (message.includes('Invalid data')) {
      return 'Некорректные данные';
    }

    return message;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched(); 
      this.error = 'Проверьте корректность данных';
      return;
    }
    
    const { email, password, name } = this.form.value;

    const request =
      this.mode === 'login'
        ? this.authService.login(email!, password!)
        : this.authService.register(email!, password!, name ?? '');

    request.subscribe({
      next: (response) => {
        if (response.user.role === 'ADMIN') {
          this.router.navigate(['/admin']);
        } else {
          this.router.navigate(['/events']);
        }
      },
      error: (err: HttpErrorResponse) => {
        const backendMessage = this.extractBackendMessage(err);

        if (backendMessage) {
          this.error = this.translateError(backendMessage);
          return;
        }

        if (this.mode === 'login') {
          if (err.status === 401) {
            this.error = 'Неверный email или пароль';
            return;
          }
        }

        if (this.mode === 'register') {
          if (err.status === 409) {
            this.error = 'Пользователь уже существует';
            return;
          }
        }

        this.error = 'Ошибка сервера';
      },
    });
  }

  togglePassword(): void {
    this.hidePassword = !this.hidePassword;
  }

}