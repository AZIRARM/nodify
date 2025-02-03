import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {User} from "../modeles/User";

@Injectable()
export class NotificationService extends Service {
  findReadedByUserId(userId: any, nbPage: number, limit: number) {
    return super.get("user/id/"+userId+"/readed?currentPage="+nbPage+"&limit="+limit);
  }
  constructor(httpClient: HttpClient) {
    super("notifications", httpClient);
  }

  getByUserId(userId:string) {
    return super.get("user/"+userId);
  }

  countUnreadedNotification(userId:string) {
    return super.get("user/id/"+userId+"/countUnreaded");
  }

  countReadedNotification(userId:string) {
    return super.get("user/id/"+userId+"/countReaded");
  }
  unreadedNotification(userId:string) {
    return super.get("user/id/"+userId+"/unreaded");
  }

  findAll() {
    return super.get("");
  }
  findPaginated(userId:string, page:number, limit:number) {
    return super.get("user/id/"+userId+"/unreaded?currentPage="+page+"&limit="+limit);
  }

  markAsNotReaded(notificationId: string, userId: string) {
    return super.post("id/" + notificationId + "/user/id/" + userId + "/markunread", null);
  }

  markAsReaded(notificationId: string, userId: string) {
    return super.post("id/" + notificationId + "/user/id/" + userId + "/markread", null);
  }

  markAllReaded(userId:string) {
    return super.post("user/id/" + userId + "/markAllAsRead", null);
  }
}
