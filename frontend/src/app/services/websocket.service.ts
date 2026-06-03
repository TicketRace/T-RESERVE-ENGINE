import { Client } from '@stomp/stompjs';
//import SockJS from 'sockjs-client';
import { Injectable } from '@angular/core';
import { Seat } from '../models/seat';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private client!: Client;

  connect(eventId: number, onSeatsUpdate: (seats: Seat[]) => void) {
    if (this.client) {
        this.client.deactivate();
      }
    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      reconnectDelay: 5000,
      onConnect: () => {
        this.client.subscribe(`/topic/seats/${eventId}`, (message) => {
          const data = JSON.parse(message.body);
          onSeatsUpdate(data.seats);
        });
      }
    });
    this.client.activate();
  }

  disconnect() {
    this.client?.deactivate();
  }
}
