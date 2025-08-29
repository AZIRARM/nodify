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

  disable(id: string) {
    return super.put("id/" + id + "/disable", null);
  }

  enable(id: string) {
    return super.put("id/" + id + "/enable", null);
  }

  activate(id: string) {
    return super.put("id/" + id +  "/activate", null);
  }

  delete(id: string) {
    return super.remove("id/"  + id);
  }

  save(plugin: Plugin) {
    return super.post("", plugin);
  }

  getDeleted() {
    return super.get("deleteds");
  }

  deleteDefinitively(id: string) {
    return super.remove("id/" + id + "/deleteDefinitively");
  }
  export(id:string) {
    return super.get("id/"+id+"/export");
  }
  import(plugin:Plugin) {
    return super.post("import", plugin);
  }

}
