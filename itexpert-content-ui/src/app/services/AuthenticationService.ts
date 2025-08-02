import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {UserLogin} from "../modeles/UserLogin";
import {jwtDecode} from "jwt-decode";
import {UserService} from "./UserService";
import {Observable, switchMap} from "rxjs";
import {User} from "../modeles/User";

@Injectable({providedIn: 'root'})
export class AuthenticationService extends Service {
  constructor(private httpClient: HttpClient,
              private userService: UserService) {
    super("authentication", httpClient);
  }

  signin(userLogin: UserLogin) {
    return super.login(userLogin);
  }

  isAuthenticated(): boolean {
    const jwtStr = localStorage.getItem('nodifyUserToken');
    if (!jwtStr) return false;

    try {
      const jwt = JSON.parse(jwtStr);
      const decoded: any = jwtDecode(jwt.accessToken);
      const now = Math.floor(Date.now() / 1000);
      return decoded.exp > now;
    } catch (error) {
      return false;
    }
  }

  getAccessToken(): string {
    const jwtStr: any = localStorage.getItem('nodifyUserToken');
    return jwtStr ? JSON.parse(jwtStr).accessToken : '';
  }

  getConnectedUser(): Observable<any> {
    return this.refresh().pipe(
      switchMap(() => {
        const email: string = this.decodeJwt().sub;
        return this.userService.getByEmail(email);
      })
    );
  }

  getRefreshToken(): string {
    const jwtStr = localStorage.getItem('nodifyUserToken');
    return jwtStr ? JSON.parse(jwtStr).refreshToken : '';
  }

  setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem("userInfo", JSON.stringify({
      token: accessToken,
      refreshToken: refreshToken
    }));
  }

  refresh(): Observable<any> {
    return super.refreshToken(this.getRefreshToken());
  }

  logout(): void {
    localStorage.removeItem('userInfo');
  }

  decodeJwt(): any {
    const payload = this.getAccessToken().split('.')[1];
    const decodedPayload = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decodedPayload);
  }
}
