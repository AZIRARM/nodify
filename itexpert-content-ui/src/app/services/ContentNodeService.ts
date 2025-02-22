import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Service} from "./Service";
import {ContentNode} from "../modeles/ContentNode";

@Injectable()
export class ContentNodeService extends Service {

  getDeleted() {
    return super.get("deleted");
  }

  deleteDefinitively(code: String) {
    return super.remove("code/" + code + "/deleteDefinitively");
  }

  constructor(httpClient: HttpClient) {
    super("content-node", httpClient);
  }

  getById(contentId: string) {
    return super.get("id/" + contentId);
  }

  getAll() {
    return super.get("");
  }

  save(content: ContentNode) {
    return super.post("", content);
  }

  delete(code: String, userId: string) {
    return super.remove("code/" + code + "/user/" + userId);
  }


  activate(code: String, userId: string) {
    return super.post("code/" + code + "/user/" + userId + "/activate", null);
  }

  getAllByParentCode(code: string) {
    return super.get("node/code/" + code);
  }

  getAllByParentCodeAndStatus(code: string, status: string) {
    return super.get("node/code/" + code + "/status/" + status);
  }

  getAllByStatus(status: string) {
    return super.get("status/" + status);
  }

  publish(contentNodeId: string, status: boolean, userId: string) {
    return super.post("id/" + contentNodeId + "/user/" + userId + "/publish/" + status, null);
  }

  getAllByCode(code: string) {
    return super.get("code/" + code);
  }

  deployVersion(code: string, version: string, userId: string) {
    return super.post("code/" + code + "/version/" + version + "/user/" + userId + "/deploy", null);
  }

  revertToVersion(code: string, version: string, userId: string) {
    return super.post("code/" + code + "/version/" + version + "/user/" + userId + "/revert", null);
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
}
