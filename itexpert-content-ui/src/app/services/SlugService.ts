import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {Role} from "../modeles/Role";
import { Observable } from 'rxjs';

@Injectable()
export class SlugService extends Service {
  constructor(httpClient: HttpClient) {
    super("slugs", httpClient);
  }

  exists(slug: string) {
    return super.get("exists/"+slug);
  }
}
