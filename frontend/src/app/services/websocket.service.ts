import { Client } from '@stomp/stompjs';
import { Injectable, NgZone } from '@angular/core';
import { Seat } from '../models/seat';
import { environment } from '../../environments/environment';

/**
 * Строим WebSocket URL:
 * - Dev:  берём из environment.wsUrl ('ws://localhost:8080')
 * - Prod: строим из window.location — wss:// для HTTPS, ws:// для HTTP.
 *         Так Railway URL подхватывается автоматически без хардкода.
 */
function getWsUrl(): string {
  if (environment.wsUrl) {
    return environment.wsUrl;
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}`;
}

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private client!: Client;

  constructor(private ngZone: NgZone) {}

  connect(eventId: number, onSeatsUpdate: (seats: Seat[]) => void) {
    if (this.client) {
        this.client.deactivate();
      }
    this.client = new Client({
      brokerURL: `${getWsUrl()}/ws/websocket`,
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('STOMP CONNECTED!');
        this.client.subscribe(`/topic/seats.${eventId}`, (message) => {
          const data = JSON.parse(message.body);
          this.ngZone.run(() => {
            onSeatsUpdate(data);
          });
        });
      },
      onWebSocketError: (err) => console.error('WS ERROR:', err),
      onStompError: (err) => console.error('STOMP ERROR:', err)
    });
    this.client.activate();
  }

  disconnect() {
    this.client?.deactivate();
  }
}
