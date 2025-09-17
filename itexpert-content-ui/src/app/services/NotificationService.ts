import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import { Observable } from 'rxjs';
import {Env} from "../../assets/configurations/environment";

@Injectable()
export class NotificationService extends Service {
  findReadedByUserId(nbPage: number, limit: number) {
    return super.get("readed?currentPage="+nbPage+"&limit="+limit);
  }
  constructor(httpClient: HttpClient) {
    super("notifications", httpClient);
  }

  markAsReaded(notificationId: string) {
    return super.post("id/" + notificationId + "/markread", null);
  }

  markAllReaded() {
    return super.post("markAllAsRead", null);
  }

  connectWebSocket(token: string, page: number = 0, limit: number = 1): Observable<any> {
    const url = `${Env.EXPERT_CONTENT_CORE_WEBSOCKET}?token=${token}&page=${page}&limit=${limit}`;

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
