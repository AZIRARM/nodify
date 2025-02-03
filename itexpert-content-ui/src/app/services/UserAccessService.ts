import {Injectable} from '@angular/core';

@Injectable()
export class UserAccessService {
  user: any;

  getUser() {
    if(!this.user) {
      this.user = JSON.parse(
        JSON.parse(
          JSON.stringify((window.localStorage.getItem('userInfo')))
        )
      );
    }
    return this.user;
  }
  canDelete() {
    let can:boolean = this.user && this.user.roles && ( this.user.roles.includes('ADMIN') ||  this.user.roles.includes('EDITOR') ) ;
    return can;
  }
  isAdmin() {
    return this.user && this.user.roles && this.user.roles.includes('ADMIN');
  }
  canCreate() {
    return this.user && this.user.roles && this.user.roles.includes('ADMIN');
  }

  canEdit() {
    let can:boolean =  this.user && this.user.roles
      && (this.user.roles.includes('ADMIN') ||  this.user.roles.includes('EDITOR'));
    return can;
  }

  canAccess(code: string) {
    return this.user && this.user.roles
      && (this.user.roles.includes('ADMIN') ||
        (this.user.projects && this.user.projects.includes(code) && (this.user.roles.includes('EDITOR') || this.user.roles.includes('READER'))));

  }

  isEditor() {
    return this.isAdmin() || this.getUser().roles.includes("EDITOR");
  }
}
