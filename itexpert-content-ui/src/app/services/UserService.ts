import {Injectable} from '@angular/core';
import {Service} from "./Service";
import {HttpClient} from "@angular/common/http";
import {User} from "../modeles/User";

@Injectable()
export class UserService extends Service {
  constructor(httpClient: HttpClient) {
    super("users", httpClient);
  }

  getAll() {
    return super.get("");
  }

  save(user: User) {
    return super.post("", user);
  }

  getByEmail(email: string) {
    return super.get("email/" + email);
  }

  changePassword(userPassword: any) {
    return super.post("password", userPassword);
  }

  getById(userId: string) {
    return super.get("id/" + userId);
  }

  setUserNamdde(content: any): void {
    if (content && !content.userName) {
      const userId = content.userId ?? content.modifiedBy;

      if (!userId) {
        console.warn("No userId provided in the content.");
        return;
      }

      this.getById(userId).subscribe({
        next: (user: any) => {
          if (user?.firstname && user?.lastname) {
            content.userName = `${user.firstname} ${user.lastname}`;
          } else {
            content.userName = 'Unknown User';
            console.warn(`User data incomplete for ID ${userId}`);
          }
        },
        error: (err: any) => {
          // Fallback name and log error
          content.userName = 'Unknown User';
          //console.error(`Failed to fetch user with ID ${userId}:`, err);
        }
      });
    }
  }

  delete(userId: string) {
    return super.remove("id/" + userId);
  }
}
