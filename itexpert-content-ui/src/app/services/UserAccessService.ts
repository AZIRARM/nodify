import {Injectable} from '@angular/core';
import {UserService} from './UserService';
import {AuthenticationService} from './AuthenticationService';
import {BehaviorSubject, Observable, of} from 'rxjs';
import {Router} from '@angular/router';
import {catchError, switchMap, tap} from 'rxjs/operators';

@Injectable({providedIn: 'root'})
export class UserAccessService {
  private userSubject = new BehaviorSubject<any | null>(null);
  user$ = this.userSubject.asObservable();
  private user: any = null;

  constructor(
    private authService: AuthenticationService,
    private userService: UserService,
    private router: Router
  ) {
    // Charge l'utilisateur au démarrage et garde l'état local + subject à jour
    this.loadUser().subscribe();
  }

  loadUser(): Observable<any> {
    return this.authService.getConnectedUser().pipe(
      switchMap((userConnected: any) => {
        if (!userConnected || !userConnected.email) {
          this.user = null;
          this.userSubject.next(null);
          this.router.navigate(['/login']);
          return of(null);
        }
        return this.userService.getByEmail(userConnected.email);
      }),
      tap((user: any) => {
        this.user = user || null;
        this.userSubject.next(this.user);
      }),
      catchError((err: any) => {
        // optionnel: log err
        this.user = null;
        this.userSubject.next(null);
        this.router.navigate(['/login']);
        return of(null);
      })
    );
  }

  getCurrentUser(): any {
    return this.user;
  }

  canEdit(): boolean {
    if (!this.user) {
      // lance un (re)chargement en arrière-plan, mais renvoie l'état actuel
      this.loadUser().subscribe();
      return false;
    }
    const roles = this.user?.roles;
    return Array.isArray(roles) && (roles.includes('ADMIN') || roles.includes('EDITOR'));
  }

  isAdmin(): boolean {
    if (!this.user) {
      this.loadUser().subscribe();
      return false;
    }
    const roles = this.user?.roles;
    return Array.isArray(roles) && roles.includes('ADMIN');
  }

  clearUser(): void {
    this.user = null;
    this.userSubject.next(null);
  }
}
