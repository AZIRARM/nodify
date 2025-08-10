import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { AuthenticationService } from '../services/AuthenticationService';
import { UserService } from '../services/UserService';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class UserStoreService {
  private userSubject = new BehaviorSubject<any | null>(null);
  user$ = this.userSubject.asObservable();

  constructor(
    private authService: AuthenticationService,
    private userService: UserService,
    private router: Router
  ) { }

  loadUser(): Observable<any> {
    return this.authService.getConnectedUser().pipe(
      switchMap((userConnected: any) => {
        if (!userConnected || !userConnected.email) {
          // Rediriger si pas d'utilisateur connecté
          this.router.navigate(['/login']);
          return of(null);
        }
        return this.userService.getByEmail(userConnected.email);
      }),
      tap((user) => {
        if (user) {
          this.userSubject.next(user);
        }
      }),
      catchError((err:any) => {
        // En cas d'erreur (ex : non connecté, token expiré, etc.)
        this.router.navigate(['/login']);
        return of(null);
      })
    );
  }

  getCurrentUser(): any {
    return this.userSubject.getValue();
  }

  clearUser() {
    this.userSubject.next(null);
  }
}
