import { Component, inject, signal, WritableSignal, OnInit } from '@angular/core';
import { UserLogin } from "../../../modeles/UserLogin";
import { Router } from "@angular/router";
import { AuthenticationService } from "../../../services/AuthenticationService";
import { CookiesService } from 'src/app/services/CookiesService';
import { LoggerService } from "../../../services/LoggerService";
import { TranslateService } from "@ngx-translate/core";
import { switchMap } from "rxjs/operators";
import { Env } from "../../../../assets/configurations/environment";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: false
})
export class LoginComponent implements OnInit {
  userLogin: WritableSignal<UserLogin> = signal<UserLogin>(new UserLogin());
  isLoading: WritableSignal<boolean> = signal(false);
  authMode: WritableSignal<string> = signal('internal');

  private authenticationService = inject(AuthenticationService);
  private cookiesService = inject(CookiesService);
  private loggerService = inject(LoggerService);
  private translate = inject(TranslateService);
  private router = inject(Router);

  hidePassword = true;

  private autoLoginTimeout: any = null;

  ngOnInit() {
    this.authenticationService.getAuthModeObservable().subscribe({
      next: (response) => {
        console.log('Auth mode received:', response.mode);
        this.authMode.set(response.mode);
      },
      error: () => {
        console.log('Auth mode error, defaulting to internal');
        this.authMode.set('internal');
      }
    });

  }

  login() {
    // Utiliser le signal authMode au lieu du service
    const currentMode = this.authMode();
    console.log('Current auth mode on login:', currentMode);

    if (currentMode === 'internal') {
      this.loginInternal();
    } else if (currentMode === 'oauth2') {
      this.authenticationService.loginWithOAuth2();
    } else if (currentMode === 'openid') {
      this.authenticationService.loginWithOpenId();
    }
  }

  private loginInternal() {
    const credentials = this.userLogin();
    console.log('Login internal with:', credentials.email);

    if (credentials.email && credentials.password) {
      this.isLoading.set(true);
      this.authenticationService.signin(credentials).pipe(
        switchMap((response) => {
          this.authenticationService.setTokens(response.token, response.refresh_token);
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
    // this.router.navigateByUrl('/');
  }

  subscribe() {
    this.router.navigateByUrl("/subscribe");
  }

  isInternalMode(): boolean {
    return this.authMode() === 'internal';
  }

  isOAuth2Mode(): boolean {
    return this.authMode() === 'oauth2';
  }

  isOpenIdMode(): boolean {
    return this.authMode() === 'openid';
  }

  subscribeEnabled(): boolean {
    return Env.SUBSCRIBE_ENABLED === 'true';
  }
}