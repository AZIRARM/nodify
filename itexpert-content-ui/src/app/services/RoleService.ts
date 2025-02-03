import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {Role} from "../modeles/Role";

@Injectable()
export class RoleService extends Service {
  constructor(httpClient: HttpClient) {
    super("users-roles", httpClient);
  }

  getAll() {
    return super.get("");
  }

  save(role: Role) {
    return super.post("", role);
  }
}
