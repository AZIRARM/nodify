import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {Role} from "../modeles/Role";
import {AccessRole} from "../modeles/AccessRole";

@Injectable()
export class AccessRoleService extends Service {
  constructor(httpClient: HttpClient) {
    super("access-roles", httpClient);
  }

  getAll() {
    return super.get("");
  }

  save(role: AccessRole) {
    return super.post("", role);
  }
}
