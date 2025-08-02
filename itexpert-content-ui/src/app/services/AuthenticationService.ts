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
     const email: string = this.decodeJwt().sub;
     return this.userService.getByEmail(email);
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
    const payload = this.getAccessToken().split('.')[1];
    const decodedPayload = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decodedPayload);
  }
}
