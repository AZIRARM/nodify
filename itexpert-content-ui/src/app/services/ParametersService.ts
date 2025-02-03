import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {Parameters} from "../modeles/Parameters";

@Injectable()
export class ParametersService extends Service {
  constructor(httpClient: HttpClient) {
    super("user-parameters", httpClient);
  }

  getByUserId(userId: string) {
    return super.get("user/" + userId);
  }

  save(parameters: Parameters) {
    return this.post("",parameters)
  }
}
