import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';
import { AuthenticationService } from '../services/AuthenticationService';
import { UserService } from '../services/UserService';

@Injectable({ providedIn: 'root' })
export class UserStoreService {
  private userSubject = new BehaviorSubject<any | null>(null);
  user$ = this.userSubject.asObservable();

  constructor(
    private authService: AuthenticationService,
    private userService: UserService
  ) {}

  loadUser(): Observable<any> {
    return this.authService.getConnectedUser().pipe(
      switchMap((userConnected: any) =>
        this.userService.getByEmail(userConnected.email)
      ),
      tap((user) => this.userSubject.next(user))
    );
  }

  getCurrentUser(): any {
    return this.userSubject.getValue();
  }

  clearUser() {
    this.userSubject.next(null);
  }
}
