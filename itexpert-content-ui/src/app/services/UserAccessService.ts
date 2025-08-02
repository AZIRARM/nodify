import {Injectable} from '@angular/core';
import {AuthenticationService} from "./AuthenticationService";
import {User} from "../modeles/User";
import {Router} from "@angular/router";

@Injectable()
export class UserAccessService {
  user!: User;

  constructor(
    private authService: AuthenticationService,
    private router: Router
  ) {
  }

  getUser(): User {
    this.init();
    return this.user;
  }
  init() {
    const session = localStorage.getItem('nodifyUserToken');
    const token = session ? JSON.parse(session).accessToken : null;

    if (!this.user ||!token || this.isTokenExpired(token)) {
      // ⚠️ Bien que getUser soit "synchrone", ici on déclenche le refresh et retourne vide si échec
      let success = false;

      this.authService.refresh().subscribe({
        next: () => {
          // Relance le chargement du user après refresh
          this.authService.getConnectedUser().subscribe({
            next: user => {
              this.user = user;
              success = true;
            },
            error: () => this.router.navigate(['/login'])
          });
        },
        error: () => this.router.navigate(['/login'])
      });

      if (!success) {
        this.router.navigate(["/login"]);
      }
    }

    if (!this.user) {
      this.router.navigate(["/login"]);
    }
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const exp = payload.exp;
      const now = Math.floor(Date.now() / 1000);
      return exp < now;
    } catch (e) {
      return true;
    }
  }

  canEdit(): boolean {
    return Array.isArray(this.user.roles)
      && (this.user.roles.includes('ADMIN') || this.user.roles.includes('EDITOR'));
  }

  isAdmin(): boolean {
    return Array.isArray(this.user.roles)
      && this.user.roles.includes('ADMIN');
  }
}
