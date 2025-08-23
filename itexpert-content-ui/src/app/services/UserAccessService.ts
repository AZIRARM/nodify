import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, of} from 'rxjs';
import {Router} from '@angular/router';
import { CookiesService } from './CookiesService';

@Injectable({providedIn: 'root'})
export class UserAccessService {
  private userSubject = new BehaviorSubject<any | null>(null);
  constructor(
    private cookiesService: CookiesService,
    private router: Router
  ) {

  }

  getCurrentUser(): any {
    const userStr = this.cookiesService.getCookie("userInfos");
    return userStr ? JSON.parse(userStr) : null;
  }

  canEdit(): boolean {
    const user:any = this.getCurrentUser();

    if (!user) {
      return false;
    }
    const roles = user?.roles;
    return Array.isArray(roles) && (roles.includes('ADMIN') || roles.includes('EDITOR'));
  }

  isAdmin(): boolean {
    const user:any = this.getCurrentUser();
    if (!user) {
      return false;
    }
    const roles = user?.roles;
    return Array.isArray(roles) && roles.includes('ADMIN');
  }

  clearUser(): void {
    this.cookiesService.eraseCookie("userToken");
    this.cookiesService.eraseCookie("userInfos");
  }
}
