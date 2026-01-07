import {Injectable} from '@angular/core';
import {Service} from "./Service";
import { interval, fromEvent, merge, Subscription, timer, forkJoin, Observable, of } from 'rxjs';
import {HttpClient} from "@angular/common/http";
import {Data} from "../modeles/Data";
import {Env} from "../../assets/configurations/environment";

@Injectable()
export class DataService extends Service {
  constructor(httpClient: HttpClient) {
    super("datas", httpClient);
  }

getByContentCode(code: string, page:number, limit:number) {
    return super.get("contentCode/" + code+"?currentPage="+page+"&limit="+limit);
  }

  save(data: Data) {
    return super.post("", data);
  }

  deleteByContentCode(code: string) {
    return super.remove("code/" + code);
  }

  delete(uuid: string) {
    return super.remove("id/" + uuid);
  }

  countDatasByContentNodeCode(code: string) {
    return super.get("contentCode/"+code+"/count");
  }

  countDatasByContentNodeCodeWebSocket(code: string): Observable<any> {
    const url = `${Env.EXPERT_CONTENT_CORE_WEBSOCKET}/datas/contentCode/?code=${code}`;

    return new Observable(observer => {

      const socket = new WebSocket(url);

      socket.onmessage = (event) => {
        observer.next(JSON.parse(event.data));
      };

      socket.onerror = (event) => {
        observer.error(event);
      };

      socket.onclose = () => {
        observer.complete();
      };

      // Cleanup à l’unsubscribe
      return () => socket.close();
    });
  }

}
