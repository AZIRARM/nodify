import {
  CanActivate,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Router,
} from '@angular/router';
import { Injectable } from '@angular/core';
import { AuthenticationService } from "./AuthenticationService";

@Injectable()
export class AuthGuard implements CanActivate {
  constructor(private authservice: AuthenticationService, private router: Router) { }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    let logged = this.authservice.isAuthenticated();

    if (!logged) {
      this.router.navigate(['/login']);
      return false;
    }

    // 🔥 Vérifier les rôles requis
    const requiredRoles = route.data['roles'] as Array<string>;

    if (requiredRoles && requiredRoles.length > 0) {
      const userRoles = this.authservice.getUserRoles();
      const hasRole = requiredRoles.some(role => userRoles.includes(role));

      if (!hasRole) {
        this.router.navigate(['/unauthorized']);
        return false;
      }
    }

    return true;
  }
}