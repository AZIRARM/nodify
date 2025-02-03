import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {Feedback} from "../modeles/Feedback";

@Injectable()
export class FeedbackService extends Service {
  constructor(httpClient: HttpClient) {
    super("feedbacks", httpClient);
  }

  getAll() {
    return super.get("");
  }

  getByContentCode(code: string) {
    return super.get("contentCode/" + code);
  }

  getByUserId(userId: string) {
    return super.get("userId/" + userId);
  }

  getByEvaluation(evaluation: number) {
    return super.get("evaluation/" + evaluation);
  }

  getByVerified(verified: boolean) {
    return super.get("verified/" + verified);
  }
  getCharts() {
    return super.get("charts");
  }

  getChartsNode(code:string) {
    return super.get("charts/node/"+code);
  }
  getChartsContent(code:string) {
    return super.get("charts/content/"+code);
  }

  save(feedback: Feedback) {
    return super.post("", feedback);
  }
}
