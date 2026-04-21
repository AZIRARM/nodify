import { Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { UserLogin } from "../modeles/UserLogin";
import { UserService } from "./UserService";
import { Observable, of, tap } from "rxjs";
import { CookiesService } from './CookiesService';
import { Env } from 'src/assets/configurations/environment';

export type AuthMode = 'internal' | 'oauth2' | 'openid';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {

  private readonly TOKEN_COOKIE = 'userToken';
  private authMode: AuthMode = 'internal';
  private baseUrl: string;

  constructor(
    private httpClient: HttpClient,
    private userService: UserService,
    private cookiesService: CookiesService
  ) {
    this.baseUrl = Env.EXPERT_CONTENT_AUTHENTICATION_URL;
    this.loadAuthMode();
  }

  private loadAuthMode(): void {
    this.httpClient.get<{ mode: AuthMode }>(`${this.baseUrl}/mode`).subscribe({
      next: (config) => {
        this.authMode = config.mode;
      },
      error: (error) => {
        console.error('Failed to load auth mode', error);
        this.authMode = 'internal';
      }
    });
  }

  getAuthMode(): AuthMode {
    return this.authMode;
  }

  getAuthModeObservable(): Observable<{ mode: AuthMode }> {
    return this.httpClient.get<{ mode: AuthMode }>(`${this.baseUrl}/mode`);
  }

  signin(userLogin: UserLogin): Observable<any> {
    if (this.authMode === 'internal') {
      return this.httpClient.post(`${this.baseUrl}/login`, userLogin);
    } else if (this.authMode === 'oauth2') {
      return this.httpClient.post(`${this.baseUrl}/oauth2/token`, userLogin);
    } else if (this.authMode === 'openid') {
      return this.httpClient.post(`${this.baseUrl}/openid/token`, userLogin);
    }
    return this.httpClient.post(`${this.baseUrl}/login`, userLogin);
  }

  loginWithOAuth2(): void {
    window.location.href = Env.EXPERT_CONTENT_AUTHENTICATION_URL + '/oauth2/authorize';
  }

  loginWithOpenId(): void {
    window.location.href = Env.EXPERT_CONTENT_AUTHENTICATION_URL + '/openid/authorize';
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
    const token = this.getAccessToken();
    console.log('Token used for loadUser:', token ? token.substring(0, 100) : 'null');

    const decoded = this.decodeJwt(token);
    console.log('Decoded token:', decoded);

    if (decoded) {
      const email = decoded.email || decoded.preferred_username || decoded.sub;
      console.log('Email extracted from token:', email);

      if (email) {
        return this.userService.getByEmail(email).pipe(
          tap((userInfos: any) => {
            console.log('User loaded from DB:', userInfos);
            this.cookiesService.setCookie("userInfos", JSON.stringify(userInfos), 1);
          })
        );
      }
    }
    return of(null);
  }

  setTokens(accessToken: string, refreshToken?: string): void {
    this.cookiesService.setCookie(
      this.TOKEN_COOKIE,
      JSON.stringify({ token: accessToken }),
      1
    );
  }

  logout(): void {
    if (this.authMode === 'internal') {
      this.cookiesService.eraseAllCookies();
    } else if (this.authMode === 'oauth2') {
      this.httpClient.post(`${this.baseUrl}/oauth2/logout`, {}).subscribe({
        next: () => {
          this.cookiesService.eraseAllCookies();
        },
        error: () => {
          this.cookiesService.eraseAllCookies();
        }
      });
    } else if (this.authMode === 'openid') {
      this.httpClient.post(`${this.baseUrl}/openid/logout`, {}).subscribe({
        next: () => {
          this.cookiesService.eraseAllCookies();
        },
        error: () => {
          this.cookiesService.eraseAllCookies();
        }
      });
    }
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

  isInternalMode(): boolean {
    return this.authMode === 'internal';
  }

  isOAuth2Mode(): boolean {
    return this.authMode === 'oauth2';
  }

  isOpenIdMode(): boolean {
    return this.authMode === 'openid';
  }

  isOAuthMode(): boolean {
    return this.authMode === 'oauth2' || this.authMode === 'openid';
  }
}