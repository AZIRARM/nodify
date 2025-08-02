import {Env} from "../../assets/configurations/environment";
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {UserLogin} from "../modeles/UserLogin";

export class Service {

  constructor(private baseRequest: string, private http: HttpClient) {
  }


  post(service: string, data: any) {
    let body = JSON.stringify(data);
    return this.http.post(Env.EXPERT_CONTENT_CORE_URL + "/" + this.baseRequest + "/" + service, body);
  }

  remove(service: string) {
    return this.http.delete(Env.EXPERT_CONTENT_CORE_URL + "/" + this.baseRequest + "/" + service);
  }

  put(service: string, data: any) {
    let body = JSON.stringify(data);
    return this.http.put(Env.EXPERT_CONTENT_CORE_URL + "/" + this.baseRequest + "/" + service, body);
  }

  get(service: string) {
    return this.http.get(Env.EXPERT_CONTENT_CORE_URL + "/" + this.baseRequest + "/" + service);
  }

  login(userLogin: UserLogin) {
    let body = JSON.stringify(userLogin);
    return this.http.post(Env.EXPERT_CONTENT_AUTHENTICATION_URL + "/login", body);
  }
  refreshToken(refreshToken: String) {
    let body = JSON.stringify({refreshToken: refreshToken});
    return this.http.post(Env.EXPERT_CONTENT_AUTHENTICATION_URL  +"/refresh", body);
  }

}
