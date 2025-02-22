import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {Data} from "../modeles/Data";

@Injectable()
export class DataService extends Service {
  constructor(httpClient: HttpClient) {
    super("datas", httpClient);
  }

getByContentCode(code: string, page:number, limit:number) {
    return super.get("contentCode/" + code+"?currentPage="+page+"&limit="+limit);
  }

  save(data: Data) {
    return super.post("", data);
  }

  deleteByContentCode(code: string) {
    return super.remove("code/" + code);
  }

  delete(uuid: string) {
    return super.remove("id/" + uuid);
  }

  countDatasByContentNodeCode(code: string) {
    return super.get("contentCode/"+code+"/count");
  }

}
