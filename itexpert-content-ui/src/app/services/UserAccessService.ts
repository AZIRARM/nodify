import {Injectable} from '@angular/core';

@Injectable()
export class UserAccessService {
  user: any;

  getUser() {
    if (!this.user) {
      this.user = JSON.parse(
        JSON.parse(
          JSON.stringify((window.localStorage.getItem('userInfo')))
        )
      );
    }
    return this.user;
  }

  canEdit() {
    let can: boolean = this.user && this.user.roles
      && (this.user.roles.includes('ADMIN') || this.user.roles.includes('EDITOR'));
    return can;
  }

  isAdmin() {
    return this.user && this.user.roles && this.user.roles.includes('ADMIN');
  }
}
