import {Env} from "../../assets/configurations/environment";
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {UserLogin} from "../modeles/UserLogin";

export class Service {

  constructor(private baseRequest: string, private http: HttpClient) {
  }


  post<T>(service: string, data: any) {
    let body = JSON.stringify(data);
    return this.http.post<T>(Env.EXPERT_CONTENT_CORE_URL + "/" + this.baseRequest + "/" + service, body);
  }

  remove<T>(service: string) {
    return this.http.delete<T>(Env.EXPERT_CONTENT_CORE_URL + "/" + this.baseRequest + "/" + service);
  }

  put<T>(service: string, data: any) {
    let body = JSON.stringify(data);
    return this.http.put<T>(Env.EXPERT_CONTENT_CORE_URL + "/" + this.baseRequest + "/" + service, body);
  }

  get<T>(service: string) {
    return this.http.get<T>(Env.EXPERT_CONTENT_CORE_URL + "/" + this.baseRequest + "/" + service);
  }

  login(userLogin: UserLogin) {
    let body = JSON.stringify(userLogin);
    return this.http.post(Env.EXPERT_CONTENT_AUTHENTICATION_URL + "/login", body);
  }

}
