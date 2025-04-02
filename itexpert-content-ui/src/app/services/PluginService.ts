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

  getAllByStatus(enable: boolean) {
    return super.get("?enabled=" + enable);
  }
  getAll() {
    return super.get("");
  }

  disable(name: string) {
    return super.put("name/" + name + "/disable", null);
  }

  enable(name: string) {
    return super.put("name/" + name + "/enable", null);
  }

  delete(id: string) {
    return super.remove("id/" + id);
  }

  save(plugin: Plugin) {
    return super.post("", plugin);
  }
}
