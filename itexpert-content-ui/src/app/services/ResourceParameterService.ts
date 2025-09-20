import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {Data} from "../modeles/Data";

@Injectable()
export class ResourceParameterService extends Service {
  constructor(httpClient: HttpClient) {
    super("resource-parameters", httpClient);
  }

  save(data: Data) {
    return super.post("", data);
  }

  deleteByContentCode(code: string) {
    return super.remove("code/" + code);
  }

  findAllResourceParameterToArchive() {
    return super.remove("/type/NODE/action/ARCHIVE");
  }
  findAll() {
    return super.get("");
  }

  delete(uuid: string) {
    return super.remove("id/" + uuid);
  }
}
