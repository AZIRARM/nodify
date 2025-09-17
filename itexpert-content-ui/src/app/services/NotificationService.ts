import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {User} from "../modeles/User";

@Injectable()
export class NotificationService extends Service {
  findReadedByUserId(nbPage: number, limit: number) {
    return super.get("readed?currentPage="+nbPage+"&limit="+limit);
  }
  constructor(httpClient: HttpClient) {
    super("notifications", httpClient);
  }

  unreaded(page:number, limit:number) {
    return super.get("unreaded?currentPage="+page+"&limit="+limit);
  }

  countUnreadedNotification() {
    return super.get("countUnreaded");
  }

  markAsReaded(notificationId: string) {
    return super.post("id/" + notificationId + "/markread", null);
  }

  markAllReaded() {
    return super.post("markAllAsRead", null);
  }
}
