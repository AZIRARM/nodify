import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {Language} from "../modeles/Language";

@Injectable()
export class LanguageService extends Service {
  constructor(httpClient: HttpClient) {
    super("languages", httpClient);
  }

  getAll() {
    return super.get("");
  }

  save(language: Language) {
    return super.post("", language);
  }

  delete(id: string) {
    return super.remove("id/" + id);
  }
}
