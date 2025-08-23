import { Injectable } from '@angular/core';
import { Service } from "./Service";
import { HttpClient } from "@angular/common/http";
import { UserLogin } from "../modeles/UserLogin";
import { UserService } from "./UserService";
import { Observable, switchMap, throwError, of } from "rxjs";
import { CookiesService } from './CookiesService';

@Injectable({ providedIn: 'root' })
@Injectable({
  providedIn: 'root'
})
export class AuthenticationService extends Service {

  private readonly TOKEN_COOKIE = 'userToken';

  constructor(
    private httpClient: HttpClient,
    private userService: UserService,
    private cookiesService: CookiesService
  ) {
    super("authentication", httpClient);
  }

  signin(userLogin: UserLogin) {
    return super.login(userLogin);
  }

  isAuthenticated(): boolean {
    const jwtStr = this.cookiesService.getCookie(this.TOKEN_COOKIE);
    if (!jwtStr) return false;

    try {
      const jwt = JSON.parse(jwtStr);
      const decoded: any = this.decodeJwt(jwt.token);
      const now = Math.floor(Date.now() / 1000);
      return decoded && decoded.exp > now;
    } catch (error) {
      return false;
    }
  }

  getAccessToken(): string {
    const jwtStr = this.cookiesService.getCookie(this.TOKEN_COOKIE);
    return jwtStr ? JSON.parse(jwtStr).token : '';
  }

  loadUser() {
    const decoded = this.decodeJwt(this.getAccessToken());

    if (decoded && decoded.sub) {  
      this.userService.getByEmail(decoded.sub).subscribe((userInfos:any)=>{
        this.cookiesService.setCookie("userInfos", JSON.stringify(userInfos), 1);
      });
    }
  }

  setTokens(accessToken: string, refreshToken: string): void {
    this.cookiesService.setCookie(
      this.TOKEN_COOKIE,
      JSON.stringify({ token: accessToken }),
      1 // nombre de jours avant expiration
    );
  }

  logout(): void {
    this.cookiesService.eraseCookie(this.TOKEN_COOKIE);
  }

  decodeJwt(token: string): any {
    if (!token || token.split('.').length !== 3) {
      return null;
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
