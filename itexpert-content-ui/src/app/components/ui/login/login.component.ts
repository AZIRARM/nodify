import {Component} from '@angular/core';
import {UserLogin} from "../../../modeles/UserLogin";
import {Router} from "@angular/router";
import {AuthenticationService} from "../../../services/AuthenticationService";
import {UserAccessService} from "../../../services/UserAccessService";
import { CookiesService } from 'src/app/services/CookiesService';
import { UserService } from 'src/app/services/UserService';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  userLogin: UserLogin = new UserLogin();

  constructor(private authenticationService: AuthenticationService,
              private cookiesService: CookiesService,
              private userService: UserService,
              private router: Router) {

  }

  login() {
    if (this.userLogin.email && this.userLogin.password) {
      this.authenticationService.signin(this.userLogin)
        .subscribe((response) => {
              this.cookiesService.setCookie("userToken", JSON.stringify(response), 1);
              this.authenticationService.loadUser();
              this.router.navigateByUrl('/nodes');
          }
        );
    }
  }

  cancel() {

  }
}
