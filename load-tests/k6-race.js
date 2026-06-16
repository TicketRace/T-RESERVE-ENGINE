import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const lockSuccess = new Counter('booking_lock_success');
const lockConflict = new Counter('booking_lock_conflict');
const lockReachedServer = new Counter('booking_reached_server');
const lockFailedOs = new Counter('booking_failed_os');
const lockDuration = new Trend('booking_lock_duration_ms', true);

const targetVus = parseInt(__ENV.VUS || '100');

export const options = {
  scenarios: {
    race_condition: {
      executor: 'shared-iterations',
      vus: targetVus,
      iterations: targetVus,
      maxDuration: '40s'
    }
  }
};

export function setup() {
  const res = http.post('http://localhost:8080/api/auth/login', JSON.stringify({ email: 'user@treserve.com', password: 'user123' }), { headers: { 'Content-Type': 'application/json' } });
  return { token: res.json('token') };
}

export default function(data) {
  const headers = { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + data.token };
  const start = Date.now();
  const res = http.post('http://localhost:8080/api/bookings/lock', JSON.stringify({ eventId: 1, seatId: 1 }), { headers });
  lockDuration.add(Date.now() - start);

  if (res.status === 0 || res.error) {
    lockFailedOs.add(1);
  } else {
    lockReachedServer.add(1);
  }

  if (res.status === 200) lockSuccess.add(1);
  else if (res.status === 409) lockConflict.add(1);
  else console.log(`UNEXPECTED STATUS: ${res.status} BODY: ${res.body}`);
}
