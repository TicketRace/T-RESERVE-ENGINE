import { Component, OnInit } from '@angular/core';
import { AdminEventSummary } from '../../models/event';
import { User } from '../../models/user';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { FormsModule } from '@angular/forms';
import { AdminCheckInService } from '../../services/admin-checkin.service';
import { Html5QrcodeScanner } from 'html5-qrcode';
import {CheckInResponse} from '../../models/checkInResponse';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css',
})
export class AdminComponent implements OnInit {
  user: User | null = null;
  events: AdminEventSummary[] = [];
  showCreatedModal = false;
  private apiUrl = environment.apiUrl;

  ticketId!: number;
  token!: string;
  checkInResult: CheckInResponse | null = null;
  showCheckIn = false;
  showScanner = false;
  scanner: Html5QrcodeScanner | null = null;
  loading = false;
  checkInMessage: string | null = null;
  notification: string | null = null;

  constructor(
    private readonly authService: AuthService,
    private readonly http: HttpClient,
    private readonly checkinService: AdminCheckInService
  ) {}

  loadEvents(): void {
    this.http.get<any>(`${this.apiUrl}/api/events`)
      .subscribe(res => {
        this.events = (res.content ?? res).map((e: any) => ({
          id: e.id,
          title: e.title,
          venue: e.venue?.name,
          nextSession: e.startTime,
        }));
      });
  }

  delete(id: number): void {
    const confirmed = confirm('Вы действительно хотите удалить это событие?');

    if (!confirmed) return;

    this.http.delete(
      `${this.apiUrl}/api/admin/events/${id}`
    ).subscribe(() => this.loadEvents());
  }

  ngOnInit(): void {
    this.user = this.authService.snapshot();
    this.loadEvents();
  }

  openCheckIn() {
    this.showCheckIn = true;
  }

  closeCheckIn() {
    this.showCheckIn = false;
    this.checkInResult = null;
    this.checkInMessage = null;
  }

  checkInById() {
    this.loading = true;

    this.checkinService.checkInById(this.ticketId).subscribe({
      next: (res) => {
        this.checkInResult = res;
        this.checkInMessage = this.formatCheckInResponse(res);
        this.loading = false;
      },
      error: (err) => {
        const normalized = this.normalizeError(err);

        this.checkInResult = normalized;
        this.checkInMessage = this.formatCheckInResponse(normalized);
        this.loading = false;
      }
    });
  }

  checkInByToken() {
    this.loading = true;

    this.checkinService.checkInByToken(this.token).subscribe({
      next: (res) => {
        this.checkInResult = res;
        this.checkInMessage = this.formatCheckInResponse(res);
        this.loading = false;
      },
      error: (err) => {
        this.showNotification(
          err.error?.message || 'Ошибка сканирования'
        );

        this.loading = false;
      }
    });
  }

  startScanner() {
    this.showScanner = true;

    setTimeout(() => {
      this.scanner = new Html5QrcodeScanner(
        "qr-reader",
        {
          fps: 10,
          qrbox: 250,
        },
        false
      );

      this.scanner.render(
        (decodedText) => {
          this.onScanSuccess(decodedText);
        },
        () => {}
      );
    }, 300);
  }

  lastScannedToken: string | null = null;
  isScanningPaused = false;

  private playSound(type: 'success' | 'error') {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const oscillator = audioCtx.createOscillator();
      const gainNode = audioCtx.createGain();

      oscillator.connect(gainNode);
      gainNode.connect(audioCtx.destination);

      if (type === 'success') {
        oscillator.type = 'sine';
        oscillator.frequency.setValueAtTime(880, audioCtx.currentTime); // A5
        gainNode.gain.setValueAtTime(0.1, audioCtx.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.3);
        oscillator.start();
        oscillator.stop(audioCtx.currentTime + 0.3);
      } else {
        oscillator.type = 'sawtooth';
        oscillator.frequency.setValueAtTime(220, audioCtx.currentTime); // A3
        oscillator.frequency.exponentialRampToValueAtTime(110, audioCtx.currentTime + 0.3);
        gainNode.gain.setValueAtTime(0.1, audioCtx.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.3);
        oscillator.start();
        oscillator.stop(audioCtx.currentTime + 0.3);
      }
    } catch (e) {
      console.warn('Web Audio API not supported', e);
    }
  }

  onScanSuccess(decodedText: string) {
    if (this.isScanningPaused) return;

    let token = decodedText.trim();

    try {
      const url = new URL(token);
      token = url.searchParams.get('token') ?? token;
    } catch {}

    try {
      const parsed = JSON.parse(token);
      token = parsed.token ?? token;
    } catch {}

    // Предотвращение двойного сканирования одного и того же QR кода подряд
    if (this.lastScannedToken === token) return;
    
    this.isScanningPaused = true;
    this.lastScannedToken = token;

    this.checkinService.checkInByToken(token).subscribe({
      next: (res) => {
        this.checkInResult = res;
        this.checkInMessage = this.formatCheckInResponse(res);
        this.playSound(res.status === 'SUCCESS' ? 'success' : 'error');
        this.resumeScannerAfterDelay();
      },
      error: (err) => {
        const normalized = this.normalizeError(err);
        this.checkInResult = normalized;
        this.checkInMessage = this.formatCheckInResponse(normalized);
        this.playSound('error');
        this.resumeScannerAfterDelay();
      }
    });
  }

  private resumeScannerAfterDelay() {
    setTimeout(() => {
      this.checkInResult = null;
      this.checkInMessage = null;
      this.lastScannedToken = null;
      this.isScanningPaused = false;
    }, 2500); // 2.5 секунды пауза перед следующим билетом
  }

  closeScanner() {
    this.scanner?.clear();
    this.scanner = null;
    this.showScanner = false;
    this.lastScannedToken = null;
    this.isScanningPaused = false;
    this.checkInResult = null;
    this.checkInMessage = null;
  }

  ngOnDestroy() {
    this.scanner?.clear();
  }

  private formatCheckInResponse(res: CheckInResponse): string {
    if (!res) return 'Нет ответа';

    if (res.status === 'SUCCESS') {
      return `Успешная проверка\nБилет: ${res.ticketId}\nСообщение: ${res.message}`;
    }

    if (res.status === 'ALREADY_USED' || res.message?.includes('used')) {
      return `Билет уже использован\nБилет: ${res.ticketId}`;
    }

    return `Ошибка: ${res.message ?? 'Неизвестная ошибка'}`;
  }

  private normalizeError(err: any): CheckInResponse {
    return {
      status: err?.error?.status ?? 'ERROR',
      ticketId: err?.error?.ticketId ?? this.ticketId,
      message: err?.error?.message ?? 'Ошибка сервера',
    };
  }

  showNotification(message: string) {
    this.notification = message;

    setTimeout(() => {
      this.notification = null;
    }, 3000);
  }
}
