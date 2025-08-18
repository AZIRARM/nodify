import { Injectable } from '@angular/core';
import { UserService } from './UserService';
import { AuthenticationService } from './AuthenticationService';
import { User } from '../modeles/User';
import { BehaviorSubject, Observable, of} from 'rxjs';
import { Router } from '@angular/router';
import { catchError, switchMap, tap } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class UserAccessService {
  private userSubject = new BehaviorSubject<any | null>(null);
  user$ = this.userSubject.asObservable();
  private user: any = null;

  constructor(
    private authService: AuthenticationService,
    private userService: UserService,
    private router: Router
  ) {
    this.loadUser().pipe(
      tap((user:any) => this.user = user)
    ).subscribe();
  }

  loadUser(): Observable<any> {
    return this.authService.getConnectedUser().pipe(
      switchMap((userConnected: any) => {
        if (!userConnected || !userConnected.email) {
          this.router.navigate(['/login']);
          return of(null);
        }
        return this.userService.getByEmail(userConnected.email);
      }),
      tap((user:any) => {
        if (user) {
          this.userSubject.next(user);
        }
      }),
      catchError((err:any) => {
        this.router.navigate(['/login']);
        return of(null);
      })
    );
  }

  getCurrentUser(): any {
    return this.user;
  }

  canEdit(): boolean {
    return !!this.user &&
      !!this.user.roles &&
      (this.user.roles.includes('ADMIN') || this.user.roles.includes('EDITOR'));
  }

  isAdmin(): boolean {
    return !!this.user &&
      !!this.user.roles &&
      this.user.roles.includes('ADMIN');
  }

  clearUser() {
    this.user = null;
    this.userSubject.next(null);
  }
}

