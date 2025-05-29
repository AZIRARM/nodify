import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";

@Injectable()
export class ChartService extends Service {
  constructor(httpClient: HttpClient) {
    super("charts", httpClient);
  }

  getCharts() {
    return super.get("");
  }
}
