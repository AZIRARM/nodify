import { Injectable } from '@angular/core';
import { Service } from "./Service";
import { HttpClient } from "@angular/common/http";
import { UserLogin } from "../modeles/UserLogin";
import { jwtDecode } from "jwt-decode";
import { UserService } from "./UserService";
import { Observable, switchMap, throwError } from "rxjs";
import { User } from "../modeles/User";

@Injectable({ providedIn: 'root' })
export class AuthenticationService extends Service {
  constructor(private httpClient: HttpClient,
    private userService: UserService) {
    super("authentication", httpClient);
  }

  signin(userLogin: UserLogin) {
    return super.login(userLogin);
  }

  isAuthenticated(): boolean {
    const jwtStr = localStorage.getItem('userToken');
    if (!jwtStr) return false;

    try {
      const jwt = JSON.parse(jwtStr);
      const decoded: any = jwtDecode(jwt.token);
      const now = Math.floor(Date.now() / 1000);
      return decoded.exp > now;
    } catch (error) {
      return false;
    }
  }

  getAccessToken(): string {
    const jwtStr: any = localStorage.getItem('userToken');
    return jwtStr ? JSON.parse(jwtStr).token : '';
  }

  getConnectedUser(): Observable<any> {
    const decoded = this.decodeJwt();

    if (!decoded || !decoded.sub) {
      return throwError(() => new Error('Token invalide ou expiré'));

      // Option 2 : rediriger directement ici (si logique UI)
      // this.router.navigate(['/login']);
      // return of(null);
    }

    return this.userService.getByEmail(decoded.sub);
  }


  setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem("userToken", JSON.stringify({
      token: accessToken
    }));
  }
  logout(): void {
    localStorage.removeItem('userToken');
  }

  decodeJwt(): any {
    const token = this.getAccessToken();

    if (!token || token.split('.').length !== 3) {
      return null; // ou throw new Error('JWT invalide');
    }

    try {
      const payload = token.split('.')[1];
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');

      const paddedBase64 = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, '=');

      const decodedPayload = atob(paddedBase64);
      return JSON.parse(decodedPayload);
    } catch (error) {
      console.error('Erreur de décodage du JWT :', error);
      return null;
    }
  }

}
