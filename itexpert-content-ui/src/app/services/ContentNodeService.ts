import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Service} from "./Service";
import {ContentNode} from "../modeles/ContentNode";
import {Node} from "../modeles/Node";

@Injectable()
export class ContentNodeService extends Service {
  constructor(httpClient: HttpClient) {
    super("content-node", httpClient);
  }

  getDeleted(parent: string) {
    const url = parent !== null ? `deleted?parent=${parent}` : 'deleted';
    return super.get(url);
  }

  deleteDefinitively(code: String) {
    return super.remove("code/" + code + "/deleteDefinitively");
  }

  deleteById(id: String) {
    return super.remove("" + id);
  }

  save(content: ContentNode) {
    return super.post("", content);
  }


  delete(code: String) {
    return super.remove("code/" + code);
  }


  activate(code: String) {
    return super.post("code/" + code + "/user/", null);
  }

  getAllByParentCodeAndStatus(code: string, status: string) {
    return super.get("node/code/" + code + "/status/" + status);
  }

  publish(contentNodeId: string, status: boolean) {
    return super.post("id/" + contentNodeId + "/publish/" + status, null);
  }

  getAllByCode(code: string) {
    return super.get("code/" + code);
  }

  deployVersion(code: string, version: string) {
    return super.post("code/" + code + "/version/" + version + "/deploy", null);
  }

  revertToVersion(code: string, version: string) {
    return super.post("code/" + code + "/version/" + version + "/revert", null);
  }

  export(code: string, environmentCode: string) {
    return super.get("code/" + code + "/export?environment=" + environmentCode);
  }

  import(parentCode: any, content: ContentNode) {
    return super.post("import?nodeParentCode=" + parentCode, content);
  }

  deploy(code: string, environmentCode: any) {
    return super.get("code/" + code + "/deploy?environment=" + environmentCode);
  }

  fillAllValuesByContentCodeStatusAndContent(param: { code: string; status: string; content: string }) {
    return super.post("code/" + param.code + "/status/" + param.status + "/fill", param);
  }

  slugExists(code: string, slug: string) {
    const safeSlug = slug?.trim() || "null";
    return super.get(`code/${encodeURIComponent(code)}/slug/${encodeURIComponent(safeSlug)}/exists`);
  }

  getByCodeAndStatus(code: string, status: string) {
    return super.get("code/" + code + "/status/" + status);
  }
}
