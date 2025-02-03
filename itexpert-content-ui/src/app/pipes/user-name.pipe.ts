import {Pipe, PipeTransform} from '@angular/core';
import {UserService} from "../services/UserService";
import {AsyncPipe} from "@angular/common";

@Pipe({
  name: 'userName'
})
export class UserNamePipe implements PipeTransform {

  constructor(private userService: UserService) {
  }

   transform(userId: any) {
     return this.userService.getById(userId).subscribe((user: any) => {
      return user.firstname + " " + user.lastname;
    });
  }

}
