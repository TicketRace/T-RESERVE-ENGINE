import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../environments/environment';
import { AdminCheckInService } from '../../services/admin-checkin.service';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { CheckInResponse } from '../../models/checkInResponse';

@Component({
  selector: 'app-admin-event-details',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './admin-event-details.html',
  styleUrl: './admin-event-details.css'
})
export class AdminEventDetailsComponent implements OnInit, OnDestroy {
  eventId!: number;
  event: any = null;

  ticketId!: number;
  token!: string;
  checkInResult: CheckInResponse | null = null;
  checkInMessage: string | null = null;
  
  showCheckIn = false;
  showScanner = false;
  scanner: Html5QrcodeScanner | null = null;
  loading = false;

  lastScannedToken: string | null = null;
  isScanningPaused = false;
  private apiUrl = environment.apiUrl;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly http: HttpClient,
    private readonly checkinService: AdminCheckInService
  ) {}

  ngOnInit(): void {
    this.eventId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadEventDetails();
  }

  loadEventDetails(): void {
    this.http.get<any>(`${this.apiUrl}/api/events/${this.eventId}`).subscribe({
      next: (res: any) => {
        this.event = res;
      },
      error: (err: any) => console.error('Failed to load event', err)
    });
  }

  openCheckIn() {
    this.showCheckIn = true;
  }

  closeCheckIn() {
    this.showCheckIn = false;
    this.checkInResult = null;
    this.checkInMessage = null;
    this.ticketId = undefined as any;
  }

  checkInById() {
    if (!this.ticketId) return;
    this.loading = true;

    this.checkinService.checkInById(this.ticketId).subscribe({
      next: (res: CheckInResponse) => {
        this.checkInResult = res;
        this.checkInMessage = this.formatCheckInResponse(res);
        this.playSound(res.status === 'USED' ? 'success' : 'error');
        this.loading = false;
      },
      error: (err: any) => {
        const normalized = this.normalizeError(err);
        this.checkInResult = normalized;
        this.checkInMessage = this.formatCheckInResponse(normalized);
        this.playSound('error');
        this.loading = false;
      }
    });
  }

  startScanner() {
    this.showScanner = true;

    setTimeout(() => {
      this.scanner = new Html5QrcodeScanner(
        "qr-reader",
        { fps: 10, qrbox: { width: 250, height: 250 } },
        false
      );

      this.scanner.render(
        this.onScanSuccess.bind(this),
        (error) => { /* ignore per frame error */ }
      );
    }, 100);
  }

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

    // ЗАЩИТА ОТ БАГА: если токен невалиден или null, сразу выдаем ошибку локально
    if (!token || token === 'null' || token === 'undefined') {
        this.isScanningPaused = true;
        this.checkInResult = {
            status: 'ERROR',
            ticketId: 0,
            eventId: this.eventId,
            eventName: this.event?.title || '',
            seatLabel: '',
            checkedInAt: new Date().toISOString()
        };
        this.checkInMessage = 'Неверный формат QR кода';
        this.playSound('error');
        this.resumeScannerAfterDelay();
        return;
    }

    if (this.lastScannedToken === token) return;
    
    this.isScanningPaused = true;
    this.lastScannedToken = token;

    this.checkinService.checkInByToken(token).subscribe({
      next: (res: CheckInResponse) => {
        this.checkInResult = res;
        this.checkInMessage = this.formatCheckInResponse(res);
        this.playSound(res.status === 'USED' ? 'success' : 'error');
        this.resumeScannerAfterDelay();
      },
      error: (err: any) => {
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
    }, 2500);
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

  private formatCheckInResponse(res: CheckInResponse): string {
    if (!res) return 'Нет ответа';

    if (res.status === 'USED') {
      return `Место ${res.seatLabel} (Билет #${res.ticketId}) успешно погашен!`;
    }

    if (res.status === 'ALREADY_USED') {
      const dateString = res.checkedInAt ?? new Date().toISOString();
      return `Билет #${res.ticketId} уже был погашен! (Сканировано в ${new Date(dateString).toLocaleTimeString()})`;
    }

    return `Ошибка: ${res.message ?? res.eventName ?? 'Неизвестная ошибка'}`;
  }

  private normalizeError(err: any): CheckInResponse {
    let status = 'ERROR';
    let message = 'Ошибка сервера';

    if (err.error) {
      if (err.error.message?.includes('уже погашен') || err.error.message?.includes('already used')) {
         status = 'ALREADY_USED';
      }
      message = err.error.message || err.error.error || err.statusText;
    } else {
      message = err.message || message;
    }

    return {
      status,
      ticketId: this.ticketId || 0,
      eventId: this.eventId,
      eventName: message, // put message here to display in formatCheckInResponse
      seatLabel: '',
      checkedInAt: new Date().toISOString()
    };
  }

  ngOnDestroy() {
    this.scanner?.clear();
  }
}
