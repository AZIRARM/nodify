import {Component} from '@angular/core';
import {UserLogin} from "../../../modeles/UserLogin";
import {Router} from "@angular/router";
import {AuthenticationService} from "../../../services/AuthenticationService";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  userLogin: UserLogin = new UserLogin();

  constructor(private authenticationService: AuthenticationService,
              private router: Router) {

  }

  login() {
    if (this.userLogin.email && this.userLogin.password) {
      this.authenticationService.signin(this.userLogin)
        .subscribe(
          (response) => {
            console.log('Réponse du login :', response);
            window.localStorage.setItem("nodifyUserToken", JSON.stringify(response));
            this.router.navigateByUrl('/nodes');
          },
        );
    }
  }

  cancel() {

  }
}
