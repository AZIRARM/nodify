import { Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Service } from "./Service";
import { Env } from "../../assets/configurations/environment";

@Injectable()
export class NotificationService extends Service {

  constructor(httpClient: HttpClient) {
    super("notifications", httpClient);
  }

  findReadedByUserId(nbPage: number, limit: number) {
    return super.get("readed?currentPage=" + nbPage + "&limit=" + limit);
  }

  markAsReaded(notificationId: string) {
    return super.post("id/" + notificationId + "/markread", null);
  }

  markAllReaded() {
    return super.post("markAllAsRead", null);
  }

  getUnreadCount(): Observable<number> {
    return super.get("count/unread").pipe(
      tap(response => console.log("Raw response from count/unread:", response)),
      map((response: any) => {
        if (typeof response === 'number') return response;
        if (response?.count !== undefined) return response.count;
        if (response?.total !== undefined) return response.total;
        console.warn("Unexpected response format:", response);
        return 0;
      }),
      tap(count => console.log("Parsed unread count:", count))
    );
  }

  connectWebSocket(token: string, page: number = 0, limit: number = 1): Observable<any> {
    const url = `${Env.EXPERT_CONTENT_CORE_WEBSOCKET}/notifications?token=${token}&page=${page}&limit=${limit}`;

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

      return () => socket.close();
    });
  }
}
