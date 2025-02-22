import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {UserLogin} from "../modeles/UserLogin";
import {jwtDecode} from "jwt-decode";

@Injectable()
export class AuthenticationService extends Service {
  constructor(httpClient: HttpClient) {
    super("authentication", httpClient);
  }

  signin(userLogin: UserLogin) {
    return super.login(userLogin);
  }

  isAuthenticated(): boolean {
    let strJwt: any = window.localStorage.getItem('userToken');

    if (strJwt) {
      let jwtToken: any = JSON.parse(strJwt);
      try {
        return jwtDecode(jwtToken.token) !== null;
      } catch (error: any) {
        return false;
      }
    }
    return false;
  }
}
