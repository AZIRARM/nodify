import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {Feedback} from "../modeles/Feedback";

@Injectable()
export class ContentDisplayService extends Service {
  constructor(httpClient: HttpClient) {
    super("content-displays", httpClient);
  }

  getCharts() {
    return super.get("charts");
  }
  getChartsByNodeCode(code:string) {
    return super.get("charts/node/"+code);
  }
  getChartsNode(code:string) {
    return super.get("charts/node/"+code);
  }
  getChartsContent(code:string) {
    return super.get("charts/content/"+code);
  }

}
