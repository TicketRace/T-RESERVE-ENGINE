import { Component, OnDestroy, OnInit, HostListener } from '@angular/core';
import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { Subject, filter, takeUntil } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule, CommonModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
})
export class NavbarComponent implements OnInit, OnDestroy {
  isLoggedIn = false;
  isAdmin = false;
  userName = '';
  isLightTheme = false;
  
  // Scroll & glassy backdrop states
  private lastScrollY = 0;
  isHidden = false;
  isScrolledOrSubpage = false;
  
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly authService: AuthService,
    readonly router: Router,
  ) {}

  ngOnInit(): void {
    // Detect theme class on body
    this.isLightTheme = document.body.classList.contains('light-theme');

    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.isLoggedIn = Boolean(user);
      this.isAdmin = user?.role === 'ADMIN';
      this.userName = user?.email ? user.email.split('@')[0] : (user?.name || '');
    });

    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntil(this.destroy$),
      )
      .subscribe(() => {
        this.isHidden = false;
        const currentScrollY = window.scrollY;
        const basePath = this.router.url.split(/[?#]/)[0];
        const isRootPage = basePath === '/' || basePath === '';
        this.isScrolledOrSubpage = !isRootPage || currentScrollY > 80;
      });

    // Initial check
    const currentScrollY = window.scrollY;
    const basePath = this.router.url.split(/[?#]/)[0];
    const isRootPage = basePath === '/' || basePath === '';
    this.isScrolledOrSubpage = !isRootPage || currentScrollY > 80;
  }

  @HostListener('window:scroll', [])
  onWindowScroll(): void {
    const currentScrollY = window.scrollY;
    
    // Hide navbar if scrolling down past 100px threshold
    if (currentScrollY > this.lastScrollY && currentScrollY > 100) {
      this.isHidden = true;
    } else if (currentScrollY < this.lastScrollY || currentScrollY <= 50) {
      // Show navbar if scrolling up or near the top
      this.isHidden = false;
    }
    
    this.lastScrollY = currentScrollY;
    
    // Glassy background ONLY on subpages OR when scrolled past the top on the root page
    const basePath = this.router.url.split(/[?#]/)[0];
    const isRootPage = basePath === '/' || basePath === '';
    this.isScrolledOrSubpage = !isRootPage || currentScrollY > 80;
  }

  toggleTheme(): void {
    this.isLightTheme = !this.isLightTheme;
    if (this.isLightTheme) {
      document.body.classList.add('light-theme');
      localStorage.setItem('theme', 'light');
    } else {
      document.body.classList.remove('light-theme');
      localStorage.setItem('theme', 'dark');
    }
  }

  goLogin(): void {
    this.router.navigate(['/login']);
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

    this.router.navigate(['/']);
  }

  goHome(): void {
    if (this.router.url !== '/') {
      this.router.navigate(['/']);
    } else {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  goEvents(): void {
    const basePath = this.router.url.split(/[?#]/)[0];
    if (basePath === '/' || basePath === '') {
      const el = document.getElementById('events-catalog');
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
      this.router.navigate(['/']).then(() => {
        setTimeout(() => {
          const el = document.getElementById('events-catalog');
          if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 100);
      });
    }
  }

  goProfile(): void {
    if (this.isAdmin) {
      this.router.navigate(['/admin']);
    } else {
      this.router.navigate(['/profile']);
    }
  }
}
