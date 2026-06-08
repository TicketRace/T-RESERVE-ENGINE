import { Component, AfterViewInit, OnDestroy, ElementRef, ViewChild, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroScene } from './hero.scene';

@Component({
  selector: 'app-hero',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './hero.component.html',
  styleUrl: './hero.component.css'
})
export class HeroComponent implements AfterViewInit, OnDestroy {
  @Output() readonly explore = new EventEmitter<void>();

  @ViewChild('canvas3d') canvas3d!: ElementRef<HTMLCanvasElement>;

  @ViewChild('canvasMobile') canvasMobile!: ElementRef<HTMLCanvasElement>;

  // Reactive Stats
  activeSessions = 847;
  countdownText = '02:14:33';
  capacityPercentage = 87;
  filledBlocksCount = 9;
  isLightTheme = false;
  btnBuyText = 'Buy Ticket via Engine';
  isTransmitting = false;

  private scene = new HeroScene();
  private sessionIntervalId: any;
  private countdownIntervalId: any;
  private themeObserver!: MutationObserver;

  constructor() {}

  ngAfterViewInit(): void {
    // Detect theme class on body
    this.isLightTheme = document.body.classList.contains('light-theme');
    
    // Initialize scene
    this.scene.init(
      this.canvas3d.nativeElement,
      null as any, // Sparkler is now global
      this.canvasMobile.nativeElement,
      null
    );
    this.scene.setTheme(this.isLightTheme);

    // Reactive MutationObserver to dynamically update Three.js theme whenever navbar toggles it on body
    this.themeObserver = new MutationObserver(() => {
      const isLight = document.body.classList.contains('light-theme');
      if (isLight !== this.isLightTheme) {
        this.isLightTheme = isLight;
        this.scene.setTheme(isLight);
      }
    });
    this.themeObserver.observe(document.body, { attributes: true, attributeFilter: ['class'] });

    // Session Counter Jitter Loop
    this.sessionIntervalId = setInterval(() => {
      const baseSessions = 847;
      const jitter = Math.floor(Math.random() * 14) - 7;
      this.activeSessions = baseSessions + jitter;
    }, 2500);

    // Ticking Countdown Interval Loop
    let totalSeconds = 2 * 3600 + 14 * 60 + 33; // 2h 14m 33s
    this.countdownIntervalId = setInterval(() => {
      if (totalSeconds <= 0) {
        clearInterval(this.countdownIntervalId);
        this.countdownText = '00:00:00';
        return;
      }
      totalSeconds--;

      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor((totalSeconds % 3600) / 60);
      const seconds = totalSeconds % 60;
      const pad = (num: number) => String(num).padStart(2, '0');

      this.countdownText = `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
    }, 1000);
  }

  ngOnDestroy(): void {
    // Disconnect MutationObserver
    if (this.themeObserver) {
      this.themeObserver.disconnect();
    }

    // Clear timeouts/intervals
    if (this.sessionIntervalId) clearInterval(this.sessionIntervalId);
    if (this.countdownIntervalId) clearInterval(this.countdownIntervalId);

    // Destroy 3D scene completely
    this.scene.destroy();
  }

  triggerEasterEgg(): void {
    this.scene.triggerEngineStart();
  }

  buyTicket(): void {
    if (this.isTransmitting) return;

    this.isTransmitting = true;
    this.btnBuyText = 'Initializing Engine CORE...';

    // Vibrate the 3D model to simulate processing
    this.scene.triggerProcessing();

    setTimeout(() => {
      this.btnBuyText = 'Engine Running ✓';
      this.capacityPercentage = 100;
      this.filledBlocksCount = 10;

      // Pulse a HUD message
      this.createHUDToast('SYSTEM ONLINE: Routing to events.', 'gold');

      setTimeout(() => {
        // Scroll smoothly to eventscatalog
        this.explore.emit();

        // Soft reset state after a delay
        setTimeout(() => {
          this.isTransmitting = false;
          this.btnBuyText = 'Buy Ticket via Engine';
          this.capacityPercentage = 87;
          this.filledBlocksCount = 9;
        }, 3000);
      }, 1000);
    }, 1800);
  }

  private createHUDToast(message: string, type: 'gold' | 'cyan'): void {
    const existing = document.querySelector('.hud-toast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = `hud-toast ${type === 'cyan' ? 'toast-cyan' : 'toast-gold'}`;
    toast.innerHTML = `
      <span class="toast-marker">▲</span>
      <span class="toast-text">${message}</span>
    `;

    if (!document.getElementById('toast-dynamic-styles')) {
      const style = document.createElement('style');
      style.id = 'toast-dynamic-styles';
      style.textContent = `
        .hud-toast {
          position: fixed;
          bottom: 40px;
          left: 64px;
          background: rgba(10, 15, 12, 0.9);
          backdrop-filter: blur(14px);
          border-radius: 8px;
          padding: 16px 24px;
          display: flex;
          align-items: center;
          gap: 12px;
          z-index: 10000;
          box-shadow: 0 10px 40px rgba(0,0,0,0.6);
          animation: slideUpHUD 0.45s cubic-bezier(0.19, 1, 0.22, 1) forwards;
          font-family: 'Orbitron', monospace;
          font-size: 11px;
          letter-spacing: 0.5px;
        }
        .hud-toast.toast-gold {
          border-left: 3px solid #cfa856;
          color: #cfa856;
        }
        .hud-toast.toast-cyan {
          border-left: 3px solid #4ce6c6;
          color: #4ce6c6;
        }
        .toast-marker {
          font-size: 12px;
        }
        @keyframes slideUpHUD {
          from { transform: translateY(40px); opacity: 0; }
          to { transform: translateY(0); opacity: 1; }
        }
        @keyframes slideDownHUD {
          from { transform: translateY(0); opacity: 1; }
          to { transform: translateY(40px); opacity: 0; }
        }
      `;
      document.head.appendChild(style);
    }

    document.body.appendChild(toast);

    setTimeout(() => {
      toast.style.animation = 'slideDownHUD 0.4s ease forwards';
      setTimeout(() => toast.remove(), 400);
    }, 4000);
  }
}
