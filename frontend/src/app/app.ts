import { Component, AfterViewInit, OnDestroy } from '@angular/core';
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
export class App implements AfterViewInit, OnDestroy {
  isAuthPage = false;
  private sparklerInitialized = false;
  private sparklerAnimationFrameId?: number;
  private mouseMoveListener?: (e: MouseEvent) => void;
  private resizeListener?: () => void;
  private scrollListener?: () => void;

  constructor(private router: Router) {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(event => {
        this.isAuthPage =
          event.urlAfterRedirects.includes('/login') ||
          event.urlAfterRedirects.includes('/register');
      });
  }

  ngAfterViewInit(): void {
    if (!this.sparklerInitialized) {
      this.initGlobalSparkler();
      this.sparklerInitialized = true;
    }
  }

  ngOnDestroy(): void {
    if (this.sparklerAnimationFrameId !== undefined) {
      cancelAnimationFrame(this.sparklerAnimationFrameId);
    }
    if (this.mouseMoveListener) {
      window.removeEventListener('mousemove', this.mouseMoveListener);
    }
    if (this.resizeListener) {
      window.removeEventListener('resize', this.resizeListener);
    }
    if (this.scrollListener) {
      window.removeEventListener('scroll', this.scrollListener);
    }
  }

  private initGlobalSparkler(): void {
    const canvas = document.getElementById('global-sparkler-canvas') as HTMLCanvasElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    this.resizeListener = (): void => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    window.addEventListener('resize', this.resizeListener);
    this.resizeListener();

    // Chaikin's Corner-Cutting Curve-Smoothing Algorithm
    const smoothPointsChaikin = (points: any[], iterations = 2): any[] => {
      if (points.length < 3) return points;
      let current = points.map((p) => ({ x: p.x, y: p.y }));
      
      for (let iter = 0; iter < iterations; iter++) {
        const next = [];
        next.push({ x: current[0].x, y: current[0].y });
        
        for (let i = 0; i < current.length - 1; i++) {
          const p0 = current[i];
          const p1 = current[i+1];
          
          const qx = p0.x * 0.75 + p1.x * 0.25;
          const qy = p0.y * 0.75 + p1.y * 0.25;
          
          const rx = p0.x * 0.25 + p1.x * 0.75;
          const ry = p0.y * 0.25 + p1.y * 0.75;
          
          next.push({ x: qx, y: qy });
          next.push({ x: rx, y: ry });
        }
        
        next.push({ x: current[current.length - 1].x, y: current[current.length - 1].y });
        current = next;
      }
      return current;
    };

    const mouseHistory: any[] = [];
    const maxHistory = 35; 
    const maxLife = 8; 

    const sparkCount = 400; 
    const sparks: any[] = [];
    for (let i = 0; i < sparkCount; i++) {
      sparks.push({
        active: false,
        x: 0,
        y: 0,
        vx: 0,
        vy: 0,
        size: 0,
        life: 0,
        maxLife: 0,
        alpha: 1.0,
        color: '#00ffcc'
      });
    }
    let sIndex = 0;

    const spawnSpark = (x: number, y: number, vx: number, vy: number, size: number, life: number, color: string): void => {
      const s = sparks[sIndex];
      s.active = true;
      s.x = x;
      s.y = y;
      s.vx = vx;
      s.vy = vy;
      s.size = size;
      s.life = life;
      s.maxLife = life;
      s.alpha = 1.0;
      s.color = color;
      sIndex = (sIndex + 1) % sparkCount;
    };

    let lastX = window.innerWidth / 2;
    let lastY = window.innerHeight / 2;
    let mouseMoved = false;

    this.mouseMoveListener = (e: MouseEvent): void => {
      // Disable sparklers when interacting with the events catalog grid
      const target = e.target as HTMLElement;
      if (target.closest('.grid-list') || target.closest('.no-results')) {
        return;
      }

      const x = e.clientX;
      const y = e.clientY;

      mouseHistory.push({ x, y, age: 0 });
      if (mouseHistory.length > maxHistory) {
        mouseHistory.shift();
      }

      const dx = x - lastX;
      const dy = y - lastY;
      const dist = Math.sqrt(dx * dx + dy * dy);

      if (dist > 0.5) {
        const nx = -dy / dist;
        const ny = dx / dist;

        const particleIntensity = Math.min(Math.floor(dist * 0.95), 18);
        for (let k = 0; k < particleIntensity; k++) {
          const ratio = k / particleIntensity;
          const spawnX = lastX + dx * ratio;
          const spawnY = lastY + dy * ratio;

          const side = Math.random() > 0.5 ? 1 : -1;
          const angle = (Math.random() - 0.5) * 0.75;

          const scatterX = (Math.cos(angle) * nx - Math.sin(angle) * ny) * side;
          const scatterY = (Math.sin(angle) * nx + Math.cos(angle) * ny) * side;

          const speedScale = 0.8 + Math.random() * 2.8;
          const vx = scatterX * speedScale + (Math.random() - 0.5) * 0.5;
          const vy = scatterY * speedScale + 0.3 + (Math.random() - 0.5) * 0.4;

          const size = 0.8 + Math.random() * 1.5;
          const life = 22 + Math.floor(Math.random() * 26);

          const colorRand = Math.random();
          let color = '#00ffcc';
          if (colorRand > 0.75) {
            color = '#ffcd54';
          } else if (colorRand > 0.45) {
            color = '#a85cfc';
          } else if (colorRand > 0.25) {
            color = '#3be68d';
          }

          spawnSpark(spawnX, spawnY, vx, vy, size, life, color);
        }
      }

      lastX = x;
      lastY = y;
      mouseMoved = true;
    };
    window.addEventListener('mousemove', this.mouseMoveListener);

    let lastScrollY = window.scrollY;
    this.scrollListener = (): void => {
      const currentScrollY = window.scrollY;
      const scrollDelta = currentScrollY - lastScrollY;
      
      if (window.innerWidth > 768) {
        const isHomePage = this.router.url.split('?')[0] === '/';
        if (isHomePage) {
          // Clear trails instantly to prevent jumping or freezing on main page scroll
          mouseHistory.length = 0;
          for (let i = 0; i < sparkCount; i++) {
            sparks[i].active = false;
          }
        } else {
          // Shift paths to match page scroll on other pages
          for (let i = 0; i < mouseHistory.length; i++) {
            mouseHistory[i].y -= scrollDelta;
          }
          for (let i = 0; i < sparkCount; i++) {
            if (sparks[i].active) {
              sparks[i].y -= scrollDelta;
            }
          }
          lastY -= scrollDelta;
        }
      }
      lastScrollY = currentScrollY;
    };
    window.addEventListener('scroll', this.scrollListener, { passive: true });

    const sparklerStartTime = performance.now();
    let renderLastScrollY = window.scrollY;

    const tickSparkler = (): void => {
      if (window.innerWidth <= 768) {
        this.sparklerAnimationFrameId = requestAnimationFrame(tickSparkler);
        return;
      }

      ctx.clearRect(0, 0, canvas.width, canvas.height);
      const time = (performance.now() - sparklerStartTime) * 0.001;

      // Zero-latency check inside render loop: if we are actively scrolling, clear trails instantly
      const currentScrollY = window.scrollY;
      if (currentScrollY !== renderLastScrollY) {
        const isHomePage = this.router.url.split('?')[0] === '/';
        if (isHomePage) {
          mouseHistory.length = 0;
          for (let i = 0; i < sparkCount; i++) {
            sparks[i].active = false;
          }
        }
        renderLastScrollY = currentScrollY;
      }

      for (let i = 0; i < mouseHistory.length; i++) {
        mouseHistory[i].age++;
      }
      while (mouseHistory.length > 0 && mouseHistory[0].age > maxLife) {
        mouseHistory.shift();
      }

      if (mouseHistory.length > 1) {
        ctx.globalCompositeOperation = 'screen';

        const smoothed = smoothPointsChaikin(mouseHistory, 4);

        if (smoothed.length > 1) {
          for (let i = 1; i < smoothed.length; i++) {
            const p1 = smoothed[i-1];
            const p2 = smoothed[i];
            const ratio = i / smoothed.length;

            ctx.strokeStyle = `rgba(0, 255, 204, ${0.05 * ratio})`;
            ctx.lineWidth = 14.0 * ratio;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';

            ctx.beginPath();
            ctx.moveTo(p1.x, p1.y);
            ctx.lineTo(p2.x, p2.y);
            ctx.stroke();
          }

          for (let i = 1; i < smoothed.length; i++) {
            const p1 = smoothed[i-1];
            const p2 = smoothed[i];
            const ratio = i / smoothed.length;

            ctx.strokeStyle = `rgba(0, 255, 204, ${0.15 * ratio})`;
            ctx.lineWidth = 5.0 * ratio;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';

            ctx.beginPath();
            ctx.moveTo(p1.x, p1.y);
            ctx.lineTo(p2.x, p2.y);
            ctx.stroke();
          }

          for (let i = 1; i < smoothed.length; i++) {
            const p1 = smoothed[i-1];
            const p2 = smoothed[i];
            const ratio = i / smoothed.length;

            ctx.strokeStyle = `rgba(255, 255, 255, ${0.75 * ratio})`;
            ctx.lineWidth = 0.8 * ratio;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';

            ctx.beginPath();
            ctx.moveTo(p1.x, p1.y);
            ctx.lineTo(p2.x, p2.y);
            ctx.stroke();
          }
        }
      }

      ctx.globalCompositeOperation = 'screen';
      ctx.shadowBlur = 0;

      for (let i = 0; i < sparkCount; i++) {
        const s = sparks[i];
        if (s.active) {
          s.life--;

          s.vx *= 0.94;
          s.vy *= 0.94;
          s.vy += 0.14;

          s.x += s.vx;
          s.y += s.vy;

          s.x += Math.sin(time * 5.0 + i) * 0.35;

          s.alpha = s.life / s.maxLife;
          
          if (s.color === '#ffcd54') {
            ctx.fillStyle = `rgba(255, 205, 84, ${s.alpha})`;
          } else if (s.color === '#a85cfc') {
            ctx.fillStyle = `rgba(168, 92, 252, ${s.alpha})`;
          } else if (s.color === '#3be68d') {
            ctx.fillStyle = `rgba(59, 230, 141, ${s.alpha})`;
          } else {
            ctx.fillStyle = `rgba(0, 255, 204, ${s.alpha})`;
          }

          ctx.beginPath();
          if (s.color === '#ffcd54' && Math.random() > 0.45) {
            ctx.rect(s.x - s.size, s.y - 0.5, s.size * 2, 1);
            ctx.rect(s.x - 0.5, s.y - s.size, 1, s.size * 2);
          } else {
            ctx.arc(s.x, s.y, s.size, 0, Math.PI * 2);
          }
          ctx.fill();

          if (s.life <= 0) {
            s.active = false;
          }
        }
      }

      mouseMoved = false;
      this.sparklerAnimationFrameId = requestAnimationFrame(tickSparkler);
    };

    tickSparkler();
  }
}