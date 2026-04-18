import { Component, inject, signal, WritableSignal } from '@angular/core';
import { UserLogin } from "../../../modeles/UserLogin";
import { Router } from "@angular/router";
import { AuthenticationService } from "../../../services/AuthenticationService";
import { CookiesService } from 'src/app/services/CookiesService';
import { LoggerService } from "../../../services/LoggerService";
import { TranslateService } from "@ngx-translate/core";
import { switchMap } from "rxjs/operators";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: false
})
export class LoginComponent {
  userLogin: WritableSignal<UserLogin> = signal<UserLogin>(new UserLogin());
  isLoading: WritableSignal<boolean> = signal(false);

  private authenticationService = inject(AuthenticationService);
  private cookiesService = inject(CookiesService);
  private loggerService = inject(LoggerService);
  private translate = inject(TranslateService);
  private router = inject(Router);

  login() {
    const credentials = this.userLogin();
    if (credentials.email && credentials.password) {
      this.isLoading.set(true);
      this.authenticationService.signin(credentials).pipe(
        switchMap((response) => {
          this.cookiesService.setCookie("userToken", JSON.stringify(response), 1);
          return this.authenticationService.loadUser();
        })
      ).subscribe({
        next: () => {
          this.router.navigateByUrl('/nodes');
          this.isLoading.set(false);
        },
        error: () => {
          this.translate.get("LOGIN_ERROR").subscribe(trad => {
            this.loggerService.error(trad);
          });
          this.isLoading.set(false);
        }
      });
    }
  }

  cancel() {
    // Optionnel: rediriger vers une page d'accueil
    // this.router.navigateByUrl('/');
  }

  subscribe() {
    this.router.navigateByUrl("/subscribe");
  }
}