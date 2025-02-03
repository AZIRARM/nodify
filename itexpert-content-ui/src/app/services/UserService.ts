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
    return super.post("password/", userPassword);
  }

  getById(userId: string) {
    return super.get("id/" + userId);
  }

  setUserName(content: any) {
    if(content && !content.userName){
      try {
        this.getById(content.userId ? content.userId : content.modifiedBy).subscribe((user: any) => {
          content.userName = user.firstname + " " + user.lastname;
        });
      }catch (error:any){
        console.error(error);
      }

    }
  }


  delete(userId: string) {
    return super.remove("id/" + userId);
  }
}
