import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {Plugin} from "../modeles/Plugin";

@Injectable()
export class PluginService extends Service {
  constructor(httpClient: HttpClient) {
    super("plugins", httpClient);
  }

  getByName(name: string) {
    return super.get("name/" + name);
  }

  getById(id: string) {
    return super.get("id/" + id);
  }

  getNotDeleted() {
    return super.get("");
  }
  getAll() {
    return super.get("");
  }

  disable(name: string, userId:string) {
    return super.put("name/" + name +"/user/"+userId+ "/disable", null);
  }

  enable(name: string, userId:string) {
    return super.put("name/" + name  +"/user/"+userId+  "/enable", null);
  }

  activate(name: string, userId:string) {
    return super.put("name/" + name  +"/user/"+userId+  "/activate", null);
  }

  delete(id: string, userId: string) {
    return super.remove("id/" + id+"/user/"+userId);
  }

  save(plugin: Plugin) {
    return super.post("", plugin);
  }

  getDeleted() {
    return super.get("deleteds");
  }

  deleteDefinitively(id: string, userId: string) {
    return super.remove("id/" + id+"/user/"+userId+"/deleteDefinitively");
  }
  export(name:string) {
    return super.get("name/"+name+"/export");
  }
  import(plugin:Plugin) {
    return super.post("import", plugin);
  }

}
