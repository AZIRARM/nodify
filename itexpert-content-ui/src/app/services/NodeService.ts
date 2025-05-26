import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {Node} from "../modeles/Node";

@Injectable()
export class NodeService extends Service {
  constructor(httpClient: HttpClient) {
    super("nodes", httpClient);
  }

  getParentsNodes(status: string) {
    return super.get("parent/status/" + status);
  }

  getPublished() {
    return super.get("published/");
  }

  getDeleted(parent: string) {
    const url = parent !== null ? `deleted?parent=${parent}` : 'deleted';
    return super.get(url);
  }

  getAllByParentCodeAndStatus(code: string, status: string) {
    return super.get("parent/code/" + code + "/status/" + status);
  }

  getAllNodes() {
    return super.get("");
  }

  save(node: Node) {
    return super.post("", node);
  }

  delete(code: String, userId: String) {
    return super.remove("code/" + code + "/user/" + userId);
  }

  deleteDefinitively(code: String) {
    return super.remove("code/" + code + "/deleteDefinitively");
  }

  activate(code: String, userId: string) {
    return super.post("code/" + code + "/user/" + userId + "/activate", null);
  }

  publish(nodeId: string, userId: string) {
    return super.post("id/" + nodeId + "/user/" + userId + "/publish", null);
  }

  getNodeByCodeAndStatus(code: string, status: string) {
    return super.get("code/" + code + "/status/" + status);
  }

  getAllNodesByCode(code: string) {
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

  import(parentCode: any, node: Node) {
    return super.post("importAll?nodeParentCode=" + parentCode, node);
  }

  haveChilds(code: any) {
    return super.get("code/" + code + "/haveChilds");
  }

  haveContents(code: any) {
    return super.get("code/" + code + "/haveContents");
  }

  deploy(code: string, environmentCode: any) {
    return super.get("code/" + code + "/deploy?environment=" + environmentCode);
  }

  getAllDescendantsByParentCode(code: string) {
    return super.get("parent/code/" + code + "/descendants");
  }

  getAllParentOrigin() {
    return super.get("origin");
  }


  slugExists(code: string, slug: string) {
    return super.get("code/" + code + "/slug/" + slug + "/exists");
  }

  getNodeView(code: string) {
    return super.get("code/" + code + "/tree-view");
  }
}
