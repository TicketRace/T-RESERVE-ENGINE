import { Component } from '@angular/core';
import { RouterOutlet, NavigationEnd, Router } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';
import { filter } from 'rxjs';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, CommonModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App {
  isAuthPage = false;

  constructor(private router: Router) {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(event => {
        this.isAuthPage =
          event.urlAfterRedirects.includes('/login') ||
          event.urlAfterRedirects.includes('/register');
      });
  }
}