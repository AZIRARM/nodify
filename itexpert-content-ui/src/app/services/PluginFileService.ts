import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {PluginFile} from "../modeles/PluginFile";

@Injectable()
export class PluginFileService extends Service {
  constructor(httpClient: HttpClient) {
    super("plugin-files", httpClient);
  }

  getPluginAssets(pluginId: string) {
    return super.get("plugin/"+pluginId);
  }


  delete(id: string) {
    return super.remove("id/" + id);
  }

  save(plugin: PluginFile) {
    return super.post("", plugin);
  }

}
