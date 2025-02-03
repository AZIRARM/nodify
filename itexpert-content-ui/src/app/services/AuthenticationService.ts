import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {Language} from "../modeles/Language";
import {UserLogin} from "../modeles/UserLogin";

@Injectable()
export class AuthenticationService extends Service {
  constructor(httpClient: HttpClient) {
    super("authentication", httpClient);
  }

  signin(userLogin: UserLogin) {
    return super.login(userLogin);
  }
  isAuthenticated(): boolean {
    if (window.localStorage.getItem('userToken')) {
      return true;
    }
    return false;
  }


}
