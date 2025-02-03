import {
  CanActivate,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Router,
} from '@angular/router';
import { Injectable } from '@angular/core';
import {AuthenticationService} from "./AuthenticationService";

@Injectable()
export class AuthGuard implements CanActivate {
  constructor(private authservice: AuthenticationService, private router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    let logged = this.authservice.isAuthenticated();

    if (logged) {
      return true;
    }
    this.router.navigate(['/login']);
    return false;
  }
}
