import {Injectable, NgZone} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Service} from "./Service";
import {AuthenticationService} from './AuthenticationService';
import {fromEvent, interval, merge, Observable, of, Subscription, timer} from 'rxjs';
import {debounceTime, switchMap} from 'rxjs/operators';
import {Env} from "../../assets/configurations/environment";

@Injectable()
export class LockService extends Service {
  private heartbeatInterval = 120000; // 2 min
  private inactivityTimeout = 300000; // 5 min
  private heartbeatSub?: Subscription;
  private inactivitySub?: Subscription;

  private currentCode?: string;


  constructor(
    private httpClient: HttpClient,
    private ngZone: NgZone,
    private authenticationService: AuthenticationService) {
    super("locks", httpClient);
  }

  getLockInfoSocket(code: string, token: string): Observable<LockInfo> {
    const self = this;
    return new Observable<LockInfo>(observer => {
      let socket: WebSocket;
      let reconnectTimeout: any;
      let isUnsubscribed = false;

      function connect() {
        if (isUnsubscribed) return;

        socket = new WebSocket(`${Env.EXPERT_CONTENT_CORE_WEBSOCKET}/owner/?code=${code}&authorization=Bearer ${token}`);

        socket.onmessage = event => {
          if (isUnsubscribed) return;
          self.ngZone.run(() => {
            try {
              observer.next(JSON.parse(event.data));
            } catch (e) {
              console.error('Parse error', e);
            }
          });
        };

        socket.onerror = err => {
          if (isUnsubscribed) return;
          self.ngZone.run(() => observer.error(err));
        };

        socket.onclose = () => {
          if (isUnsubscribed) return;
          reconnectTimeout = setTimeout(connect, 3000);
        };
      }

      connect();

      return () => {
        isUnsubscribed = true;
        clearTimeout(reconnectTimeout);
        if (socket) {
          socket.onclose = null;
          socket.onerror = null;
          socket.onmessage = null;
          socket.close();
        }
      };
    });
  }

  // --- Acquérir un lock ---
  acquire(code: string) {
    this.currentCode = code;
    return super.post(`acquire/${code}`, {}).pipe(
      switchMap(acquired => {
        if (acquired) {
          this.startHeartbeat();
          this.startInactivityWatcher();
        }
        return of(acquired);
      })
    );
  }

  // --- Relâcher le lock ---
  release(): void {
    if (!this.currentCode) return;

    this.stopHeartbeat();
    this.stopInactivityWatcher();

    super.post(`release/${this.currentCode}`, {}).subscribe();
  }

  // --- Heartbeat pour prolonger le TTL ---
  private startHeartbeat(): void {
    this.stopHeartbeat();
    this.heartbeatSub = interval(this.heartbeatInterval).subscribe(() => {
      if (this.currentCode) {
        super.post(`refresh/${this.currentCode}`, {}).subscribe();
      }
    });
  }

  adminRelease(code: string): Observable<any> {
    return super.post(`admin/release/${code}`, {});
  }


  private stopHeartbeat(): void {
    if (this.heartbeatSub) {
      this.heartbeatSub.unsubscribe();
      this.heartbeatSub = undefined;
    }
  }

  private stopInactivityWatcher(): void {
    if (this.inactivitySub) {
      this.inactivitySub.unsubscribe();
      this.inactivitySub = undefined;
    }
  }

  // --- Inactivité utilisateur (configurable) ---
  startInactivityWatcher(timeout: number = this.inactivityTimeout, onTimeout?: () => void): void {
    this.stopInactivityWatcher();

    this.ngZone.runOutsideAngular(() => {
      const activityEvents = merge(
        fromEvent(document, 'mousemove'),
        fromEvent(document, 'keydown'),
        fromEvent(document, 'mousedown'),
        fromEvent(document, 'touchstart')
      );

      this.inactivitySub = activityEvents.pipe(
        debounceTime(500),
        switchMap(() => timer(timeout))
      ).subscribe(() => {
        this.ngZone.run(() => {
          this.release();
          if (onTimeout) onTimeout();
        });
      });
    });
  }

  handleAllLocks(): Observable<LockInfo[]> {
    const self = this;
    const token = this.authenticationService.getAccessToken();
    const url = `${Env.EXPERT_CONTENT_CORE_WEBSOCKET}/lock-contents?authorization=Bearer ${encodeURIComponent(token)}`;

    return new Observable<LockInfo[]>(observer => {
      let socket: WebSocket;
      let reconnectTimeout: any;
      let isUnsubscribed = false;

      function connect() {
        if (isUnsubscribed) return;

        socket = new WebSocket(url);

        socket.onmessage = (event) => {
          if (isUnsubscribed) return;
          self.ngZone.run(() => {
            try {
              const locksList = JSON.parse(event.data);
              observer.next(locksList);
            } catch (e) {
              console.error('Parse error', e);
            }
          });
        };

        socket.onerror = (event) => {
          if (isUnsubscribed) return;
          self.ngZone.run(() => observer.error(event));
        };

        socket.onclose = () => {
          if (isUnsubscribed) return;
          reconnectTimeout = setTimeout(connect, 3000);
        };
      }

      connect();

      return () => {
        isUnsubscribed = true;
        clearTimeout(reconnectTimeout);
        if (socket) {
          socket.onclose = null;
          socket.onerror = null;
          socket.onmessage = null;
          socket.close();
        }
      };
    });
  }
}
