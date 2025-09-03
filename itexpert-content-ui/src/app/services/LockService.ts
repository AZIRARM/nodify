import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Service } from "./Service";
import { interval, fromEvent, merge, Subscription, timer, forkJoin, Observable, of } from 'rxjs';
import { debounceTime, switchMap } from 'rxjs/operators';

@Injectable()
export class LockService extends Service {
  private heartbeatInterval = 120000; // 2 min
  private inactivityTimeout = 300000; // 5 min
  private heartbeatSub?: Subscription;
  private inactivitySub?: Subscription;

  private currentCode?: string;

  constructor(httpClient: HttpClient, private ngZone: NgZone) {
    super("locks", httpClient);
  }

  // --- Récupère le lock pour un node ---
  getLockInfo(code: string) {
    return super.get(`owner/${code}`);
  }

  // --- Récupère les locks de plusieurs nodes ---
  getLockInfos(codes: string[]){
    if (!codes?.length) return of({});
    const calls: Record<string, any> = {};
    codes.forEach((code:string) => calls[code] = this.getLockInfo(code));
    return forkJoin(calls);
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

    adminRelease(code: string) {
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

}
