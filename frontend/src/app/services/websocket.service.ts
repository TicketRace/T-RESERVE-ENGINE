import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Injectable, NgZone } from '@angular/core';
import { Seat } from '../models/seat';
import { environment } from '../../environments/environment';

/**
 * Строим SockJS HTTP URL для fallback:
 * - Dev:  берём из environment.apiUrl ('http://localhost:8080')
 * - Prod: строим из window.location
 */
function getSockJsUrl(): string {
  if (environment.apiUrl) {
    return `${environment.apiUrl}/ws`;
  }
  return `${window.location.protocol}//${window.location.host}/ws`;
}

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private client!: Client;

  constructor(private ngZone: NgZone) { }

  connect(eventId: number, onSeatsUpdate: (seats: Seat[]) => void) {
    if (this.client) {
      this.client.deactivate();
    }
    this.client = new Client({
      // SockJS автоматически попытается использовать WebSocket,
      // а если не выйдет (прокси блокирует) — переключится на HTTP Polling
      // webSocketFactory: () => new SockJS(getSockJsUrl()) as any,
      webSocketFactory: () => new SockJS(getSockJsUrl(), null, {
        transports: ['websocket', 'xhr-streaming', 'xhr-polling']
      }) as any,
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('STOMP CONNECTED (with SockJS fallback)!');
        this.client.subscribe(`/topic/seats.${eventId}`, (message) => {
          const data = JSON.parse(message.body);
          this.ngZone.run(() => {
            onSeatsUpdate(data);
          });
        });
      },
      onWebSocketError: (err) => console.error('WS/SockJS ERROR:', err),
      onStompError: (err) => console.error('STOMP ERROR:', err)
    });
    this.client.activate();
  }

  disconnect() {
    this.client?.deactivate();
  }
}

