import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterLinkActive, RouterModule } from '@angular/router';
import { Subject, filter, takeUntil } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule, RouterLinkActive, CommonModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
})
export class NavbarComponent implements OnInit, OnDestroy {
  isLoggedIn = false;
  isAdmin = false;
  userName = '';
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly authService: AuthService,
    readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.isLoggedIn = Boolean(user);
      this.isAdmin = user?.role === 'ADMIN';
      this.userName = user?.name ?? '';
    });

    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  logout(): void {
    localStorage.removeItem('token');
    this.authService.logout();

    this.isLoggedIn = false;
    this.isAdmin = false;
    this.userName = '';

    this.router.navigate(['/login']);
  }

  goProfile(): void {
    this.router.navigate(['/profile']);
  }
}
