import { Injectable } from '@angular/core';
import { UserService } from './UserService';
import { AuthenticationService } from './AuthenticationService';
import { User } from '../modeles/User';
import { Observable, switchMap } from 'rxjs';
import { UserStoreService } from '../stores/UserStoreService';
import { tap } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class UserAccessService {
  private user: any = null;

  constructor(private userStore: UserStoreService) {
    this.userStore.loadUser().pipe(
      tap(user => this.user = user)
    ).subscribe();
  }

  get user$(): Observable<any> {
    return this.userStore.user$;
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
}
